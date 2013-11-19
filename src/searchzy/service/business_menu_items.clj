(ns searchzy.service.business-menu-items
  (:use [clojure.core.match :only (match)])
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [metadata :as meta]
             [flurbl :as flurbl]
             [util :as util]
             [inputs :as inputs]
             [responses :as responses]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(defn- mk-one-hit
  "Replace :hours with :hours_today, using :day_of_week.
   Add :distance_in_mi."
  [source-map day-of-week coords]
  (let [old-biz     (:business source-map)
        hours-today (util/get-hours-for-day (:hours old-biz) day-of-week)
        dist        (util/haversine (:coordinates old-biz) coords)
        new-biz     (assoc (dissoc old-biz
                                   :hours :latitude_longitude
                                   :rails_time_zone
                                   :yelp_star_rating
                                   :yelp_review_count
                                   :yelp_id)
                      :yelp {:id (:yelp_id old-biz)
                             :star_rating (:yelp_star_rating old-biz)
                             :review_count (:yelp_review_count old-biz)}
                      :hours_today hours-today
                      :distance_in_mi dist)]
    (assoc
        (dissoc source-map
                :latitude_longitude
                :yelp_star_rating
                :yelp_review_count)
      :business new-biz)))

;; TODO: Add earliest_open.

(defn- mk-response
  "From ES response, create service response.
   We haven't done paging yet (because we needed metadata),
   so we need to do paging here."
  [results metadata day-of-week {:keys [item-id geo-map hours-map
                                        utc-offset-map sort-map page-map]}]
  (let [pageful     (take (:size page-map) (drop (:from page-map) results))
        resp-hits   (map #(mk-one-hit % day-of-week (:coords geo-map))
                         (map :_source pageful))]
    (responses/ok-json
     {:endpoint "/v1/business_menu_items"   ; TODO: pass this in
      :arguments {:item_id item-id
                  :geo_filter geo-map
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

(def idx-name (:index (:business_menu_items cfg/elastic-search-names)))
(def mapping-name (:mapping (:business_menu_items cfg/elastic-search-names)))

(defn value-sort
  [order]
  (array-map :yelp_star_rating  order
             :yelp_review_count order
             :price_micros      (if (= order :asc) :desc :asc)
             :value_score_picos order
             :value_score_int   order))

(def DEFAULT_SORT (value-sort :desc))

(defn mk-sort
  [sort-map geo-map]
  (let [order (:order sort-map)]
    (match (:attribute sort-map)
           "value"    (value-sort order)
           "distance" (flurbl/mk-geo-distance-sort-builder
                       (:coords geo-map) order)
           "price"    {:price_micros order}
           :else      DEFAULT_SORT)))

(defn- sort-by-distance?
  [sort-map]
  (= "distance" (:attribute sort-map)))

(defn- es-search
  "Perform search against item_id."
  [item-id geo-map sort-map page-map]
  (let [search-fn (if (sort-by-distance? sort-map)
                    flurbl/distance-sort-search
                    es-doc/search)]
    (:hits
     (search-fn idx-name mapping-name
                :query  {:field {:item_id item-id}}
                :filter (util/mk-geo-filter geo-map)
                :sort   (mk-sort sort-map geo-map)
                :from   (:from page-map)
                :size   (:size page-map)))))

(defn- filter-by-hours
  [hours-map biz-menu-items]
  (if (= {} hours-map)
    biz-menu-items
    (filter #(util/open-at? hours-map (-> % :_source :business :hours))
            biz-menu-items)))

(def MAX_ITEMS 1000)
(defn get-all-open-items
  [{:keys [item-id geo-map hours-map sort-map]}]
  ;; Do search, getting lots of (MAX_ITEMS) results.
  ;; Return *only* the actual hits, losing the actual number of results!
  (let [{items :hits} (es-search item-id geo-map sort-map
                                 {:from 0, :size MAX_ITEMS})]
    ;; Possible post-facto filter using hours-map.
    (filter-by-hours hours-map items)))
  

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
            day-of-week (util/get-day-of-week
                         (:hours-map valid-args)
                         (some #(-> % :_source :business :rails_time_zone) items)
                         (:utc-offset-map valid-args))
            ;; Gather metadata from the results returned.
            metadata (meta/get-metadata items day-of-week)]
        ;; Create JSON response.
        (mk-response items metadata day-of-week valid-args)))))
