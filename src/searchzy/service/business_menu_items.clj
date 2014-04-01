(ns searchzy.service.business-menu-items
  "A new version of business-menu-items which employs a 'collar'.
   That is, it always performs sort-by-distance searches,
   and then it 'takes' from them until satisfying 'min_results'.
   or simply running out."
  (:use [clojure.core.match :only (match)]
        [camel-snake-kebab])
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [value :as value]
             [metadata :as meta]
             [geo-sort :as geo-sort]
             [util :as util]
             [inputs :as inputs]
             [responses :as responses]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(defn- mk-one-hit
  "Replace :hours with :hours_today, using :day_of_week.
   Add in Yelp data."
  [result day-of-week coords]
  (let [source-map  result ;; (:_source result)
        old-biz     (:business source-map)
        hours-today (util/get-hours-for-day (:hours old-biz) day-of-week)
        new-biz     (-> old-biz
                        (dissoc :hours
                                :latitude_longitude
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
        (dissoc :latitude_longitude
                :yelp_star_rating
                :yelp_review_count)
        (assoc :business new-biz
               ;; FIXME: awesomeness isn't from :_source!
               :awesomeness (:awesomeness result)))))

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
  (map :_source
       (:hits
        (:hits
         (es-doc/search idx-name mapping-name
                        :query  {:field {:item_id item-id}}
                        :filter (util/mk-geo-filter geo-map)
                        :sort   (geo-sort/mk-geo-distance-sort-builder
                                 (:coords geo-map) :asc)
                        :from   (:from page-map)
                        :size   (:size page-map))))))

(defn- maybe-filter-by-hours
  [hours-map items]
  (if (= {} hours-map)
    items
    ;; (filter #(util/open-at? hours-map (-> % :_source :business :hours))
    (filter #(util/open-at? hours-map (-> % :business :hours))
            items)))

(defn- maybe-reverse
  [sort-map items]
  (if (= :desc (:order sort-map))
    items
    (reverse items)))

(defn- maybe-re-sort
  [sort-map items]
  (match (:attribute sort-map)
         ;; "distance" items   ;; Already handles 'reverse' within ES.
         "distance" (->> items
                         (sort-by #(-> % :business :distance_in_mi))
                         (maybe-reverse sort-map))
         "price"    (->> items
                         ;; (sort-by #(-> % :_source :price_micros))
                         (sort-by (fn [i] [(:price_micros i)
                                           (- 0 (-> i :business :distance_in_mi)) ]))
                         (maybe-reverse sort-map))
         "value"    (->> items
                         value/score-and-sort
                         (maybe-reverse sort-map))))

(defn- add-distance-in-mi
  [item coords]
  ;; (let [item-coords (-> item :_source :business :coordinates)
  (let [item-coords (-> item :business :coordinates)
        dist        (util/haversine item-coords coords)]
    ;; (assoc-in item [:_source :business :distance_in_mi] dist)))
    (assoc-in item [:business :distance_in_mi] dist)))

(defn- maybe-collar
  "Take only the closest results, stopping when you:
      - have enough AND you're at least 1m out  -OR-
      - run out."
  ;; [geo-map collar-map items]
  [collar-map items]
  (let [mr (:min-results collar-map)
        miles 1.0]
    (if (nil? mr)
      items
      ;; (let [get-dist    #(-> % :_source :business :distance_in_mi)
      (let [get-dist    #(-> % :business :distance_in_mi)
            need-more? (fn [[item idx]]
                         (or (< idx mr)
                             (< (get-dist item) miles)))
            ;; add-dist    #(add-distance-in-mi % (:coords geo-map))]
            ]
        (map first
             (take-while need-more?
                         (map vector
                              items ;; (map add-dist items)
                              (iterate inc 0))))))))

(defn- most-common
  ":: [a] -> a"
  [seq]
  (first
   (reduce
    (fn [[k1 v1] [k2 v2]]
      (if (> v2 v1)
        [k2 v2]
        [k1 v1]))
    (frequencies seq))))

(defn- get-category-id
  ":: [es-item] -> str
   Given BusinessMenuItem ES search results,
   Gather up all the business_category_ids.
   Use those to make a NEW search."
  [items item-id geo-map sort-map]
  (let [at-least-one-item
        (if (empty? items) 
          (es-search item-id (assoc geo-map :max_miles 100) sort-map {:from 0, :size 1})
          items)]
    (most-common (mapcat #(-> % :business :business_category_ids) at-least-one-item))))

;; Rather than search the 'business_menu_items' index,
;; we need to search the 'businesses' index.
(defn- es-by-cat-id-search
  "Perform search against BusinessCategory _id, specifically using:
     - geo-distance sort  -AND-
     - geo-distance filter"
  [cat-id geo-map sort-map page-map]
  (let [names (:businesses cfg/elastic-search-names)]
    (:hits
     (es-doc/search (:index names) (:mapping name)
                    :query {:field {:business_category_ids cat-id}}
                    :filter (util/mk-geo-filter geo-map)
                    :sort   (geo-sort/mk-geo-distance-sort-builder
                             (:coords geo-map) :asc)
                    :from   (:from page-map)
                    :size   (:size page-map)))))

(defn- de-dupe
  "If a biz in bizs is already in items, then remove from bizs."
  [bizs items]
  ;; (let [biz-ids (map #(:_id (:business (:_source %))) items)]
  (let [biz-ids (map #(-> % :business :_id) items)]
    (remove #((set biz-ids) (:_id %)) bizs)))

;; use 'partial'
(defn- add-distances
  [geo-map items]
  (map #(add-distance-in-mi % (:coords geo-map)) items))

(defn- filter-collar-sort
  [items geo-map collar-map hours-map sort-map]
  (->> items
       (add-distances geo-map)
       (maybe-filter-by-hours hours-map)
       (maybe-collar collar-map)
       (maybe-re-sort sort-map)))

(defn- mk-biz-into-item-like-thing
  [biz]
  (let [just-biz (:_source biz)]
    {:business (assoc just-biz :_id (:_id biz))}))

(defn- get-at-least-one-item
  [item-id new-geo-map sort-map fake-pager]
  (let [items (es-search item-id new-geo-map sort-map fake-pager)]
    (if (empty? items)
      (es-search item-id (assoc new-geo-map :miles 100) sort-map {:from 0, :size 1})
      items)))

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
        fake-pager {:from 0, :size MAX-ITEMS}
        ;; items (es-search item-id new-geo-map sort-map fake-pager)
        items (es-search item-id new-geo-map sort-map fake-pager)

        ;; If (empty? items), then we're screwed.
        ;; Need to have some item so we know the category-id.
        category-id (get-category-id items item-id geo-map sort-map)
        {category-bizs :hits} (es-by-cat-id-search category-id new-geo-map
                                                   sort-map fake-pager)
        uniq-bizs (map mk-biz-into-item-like-thing (de-dupe category-bizs items))]

    (filter-collar-sort (concat items uniq-bizs) geo-map collar-map hours-map sort-map)))

    ;; (if (= (:attribute sort-map) "distance")
    ;;   ;; If geo-sort, first concat lists.  Then filter, collar, and sort.
    ;;   (filter-collar-sort (concat items uniq-bizs) geo-map collar-map hours-map sort-map)
    ;;   ;; Otherwise: filter, collar, and sort 'items', THEN concat.
    ;;   (concat (filter-collar-sort items geo-map collar-map hours-map sort-map) uniq-bizs))))

(defn- get-day-of-week
  [items valid-args]
  (util/get-day-of-week
   (:hours-map valid-args)
   ;; (some #(-> % :_source :business :rails_time_zone) items)
   (some #(-> % :business :rails_time_zone) items)
   (:utc-offset-map valid-args)))

(defn- search
  [valid-args]
  (let [items (get-all-open-items valid-args)
        day-of-week (get-day-of-week items valid-args)
        ;; Gather metadata from the results returned.
        metadata (meta/get-metadata items day-of-week)]
    (mk-response items metadata day-of-week valid-args)))

(defn validate-and-search
  ""
  [input-args]
  (util/validate-and-search input-args inputs/biz-menu-item-clean-input search))
