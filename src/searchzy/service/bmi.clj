(ns searchzy.service.bmi
  (:use [clojure.core.match :only (match)]
        [camel-snake-kebab])
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [value :as value]
             [metadata :as meta]
             [flurbl :as flurbl]
             [util :as util]
             [inputs :as inputs]
             [responses :as responses]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(defn- mk-one-hit
  "Replace :hours with :hours_today, using :day_of_week."
  [result day-of-week coords]
  (let [source-map  (:_source result)
        old-biz     (:business source-map)
        hours-today (util/get-hours-for-day (:hours old-biz) day-of-week)
        new-biz     (-> old-biz
                        (dissoc :hours :latitude_longitude
                                :rails_time_zone
                                :yelp_star_rating
                                :yelp_review_count
                                :yelp_id)
                        (assoc 
                            :yelp {:id (:yelp_id old-biz)
                                   :star_rating (:yelp_star_rating old-biz)
                                   :review_count (:yelp_review_count old-biz)}
                            :hours_today hours-today
                            ))]
    (-> source-map
        (assoc :business new-biz
               :awesomeness (:awesomeness result))
        (dissoc :latitude_longitude
                :yelp_star_rating
                :yelp_review_count))))

(defn map-keys [f m]
  (letfn [(mapper [[k v]] [(f k) (if (map? v) (map-keys f v) v)])]
    (into {} (map mapper m))))

(defn- mk-response
  "From ES response, create service response.
   We haven't done paging yet (because we needed metadata),
   so we need to do paging here."
  [results metadata day-of-week {:keys [item-id geo-map collar-map hours-map
                                        utc-offset-map sort-map page-map]}]
  (let [pageful   (take (:size page-map) (drop (:from page-map) results))
        resp-hits (map #(mk-one-hit % day-of-week (:coords geo-map)) pageful)]
    (responses/ok-json
     {:endpoint "/v1/business_menu_items"   ; TODO: pass this in
      :arguments {:item_id item-id
                  :geo_filter (assoc geo-map :collar
                                     (map-keys ->snake_case_keyword collar-map))
                  :hours_filter hours-map
                  :utc_offset utc-offset-map
                  :day_of_week day-of-week
                  :sort sort-map
                  :paging page-map}
      :results {:count (count results)
                :prices_micros (:prices-micros metadata)
                :latest_close (:latest-close metadata)
                :hits resp-hits
                }})))

(def idx-name     (:index   (:business_menu_items cfg/elastic-search-names)))
(def mapping-name (:mapping (:business_menu_items cfg/elastic-search-names)))

(defn- es-search
  "Perform search against item_id, specifically using:
     - geo-distance sort  -AND-
     - geo-distance filter"
  [item-id geo-map sort-map page-map]
  (:hits
   (es-doc/search idx-name mapping-name
                  :query  {:field {:item_id item-id}}
                  :filter (util/mk-geo-filter geo-map)
                  :sort   (flurbl/mk-geo-distance-sort-builder
                           (:coords geo-map) :asc)
                  :from   (:from page-map)
                  :size   (:size page-map))))

(defn- maybe-filter-by-hours
  [hours-map items]
  (if (= {} hours-map)
    items
    (filter #(util/open-at? hours-map (-> % :_source :business :hours))
            items)))

(defn- maybe-reverse
  [sort-map items]
  (if (= :desc (:order sort-map))
    items
    (reverse items)))

(defn- maybe-re-sort
  [sort-map items]
  (match (:attribute sort-map)
         "distance" items   ;; Already handles 'reverse' within ES.
         "price"    (->> items
                         (sort-by #(-> % :_source :price_micros))
                         (maybe-reverse sort-map))
         "value"    (->> items
                         value/score-and-sort
                         (maybe-reverse sort-map))))

(defn- add-distance-in-mi
  [item coords]
  (let [item-coords (-> item :_source :business :coordinates)
        dist        (util/haversine item-coords coords)]
    (assoc-in item [:_source :business :distance_in_mi] dist)))

(defn- maybe-collar
  "Take only the closest results, stopping when you:
      - have enough AND you're at least 1m out  -OR-
      - run out."
  [geo-map collar-map items]
  (let [mr (:min-results collar-map)
        miles 1.0]
    (if (nil? mr)
      items
      (let [get-dist    #(-> % :_source :business :distance_in_mi)
            need-more? (fn [[item idx]]
                         (or (< idx mr)
                             (< (get-dist item) miles)))
            add-dist    #(add-distance-in-mi % (:coords geo-map))]
        (map first
             (take-while need-more?
                         (map vector
                              (map add-dist items)
                              (iterate inc 0))))))))

(def MAX-ITEMS 1000)
(defn- get-all-open-items
  "Filter by max_miles, if present.  Sort by distance.
   Then in-process, do additional filtering, collaring, and sorting as needed."
  [{:keys [item-id geo-map collar-map hours-map sort-map]}]
  ;; Do search, getting lots of (MAX_ITEMS) results.
  ;; Return *only* the actual hits, losing the actual number of results!
  (let [new-geo-map (if (:max-miles collar-map)
                      (assoc geo-map :miles (:max-miles collar-map))
                      geo-map)
        {items :hits} (es-search item-id new-geo-map
                                 sort-map {:from 0, :size MAX-ITEMS})]
    ;; in-process...
    (->> items
         (maybe-filter-by-hours hours-map)
         (maybe-collar geo-map collar-map)
         (maybe-re-sort sort-map))))

(defn get-day-of-week
  [items valid-args]
  (util/get-day-of-week
   (:hours-map valid-args)
   (some #(-> % :_source :business :rails_time_zone) items)
   (:utc-offset-map valid-args)))

(def sort-attrs #{"price" "value" "distance"})
(defn validate-and-search
  ""
  [input-args]
  (let [[valid-args errs] (inputs/biz-menu-item-clean-input input-args sort-attrs)]
    (if (seq errs)
      ;; Validation error.
      (responses/error-json {:errors errs})
      ;; Do ES search.
      (let [items (get-all-open-items valid-args)
            day-of-week (get-day-of-week items valid-args)
            ;; Gather metadata from the results returned.
            metadata (meta/get-metadata items day-of-week)]
        ;; Create JSON response.
        (mk-response items metadata day-of-week valid-args)))))
