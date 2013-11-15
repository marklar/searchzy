(ns searchzy.service.business-menu-items
  (:use [clojure.core.match :only (match)])
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [metadata :as meta]
             [flurbl :as flurbl]
             [util :as util]
             [inputs :as inputs]
             [validate :as validate]
             [geo :as geo]
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
  "From ES response, create service response."
  [results metadata item-id geo-map hours-map sort-map page-map]
  (let [day-of-week (util/get-day-of-week)
        pageful   (take (:size page-map) (drop (:from page-map) results))
        resp-hits (map #(mk-one-hit % day-of-week (:coords geo-map))
                       (map :_source pageful))]
    (responses/ok-json
     {:endpoint "/v1/business_menu_items"   ; TODO: pass this in
      :arguments {:item_id item-id
                  :geo_filter geo-map
                  :hours_filter hours-map
                  :sort sort-map
                  :paging page-map
                  :day_of_week day-of-week}
      :results {:count (count results)
                :prices_micros (:prices-micros metadata)
                :latest_close (:latest-close metadata)
                :hits resp-hits
                }})))

(def idx-name (:index (:business_menu_items cfg/elastic-search-names)))
(def mapping-name (:mapping (:business_menu_items cfg/elastic-search-names)))

(def DEFAULT_SORT {:value_score_int :desc})

(defn mk-sort
  [sort-map geo-map]
  (let [order (:order sort-map)]
    (match (:attribute sort-map)
           "value"    {:value_score_int order}
           "distance" (flurbl/mk-geo-distance-sort-builder (:coords geo-map) order)
           "price"    {:price_micros order}
           :else      DEFAULT_SORT)))

;; :sort   (array-map :yelp_star_rating  :desc
;;                    :yelp_review_count :desc
;;                    :value_score_picos :desc)

(defn- sort-by-distance?
  [sort-map]
  (= "distance" (:attribute sort-map)))

(defn- get-items
  "Perform search against item_id."
  [item-id geo-map sort-map page-map]
  (let [search-fn (if (sort-by-distance? sort-map)
                    flurbl/distance-sort-search
                    es-doc/search)]
    (:hits (:hits
            (search-fn idx-name mapping-name
                       :query  {:field {:item_id item-id}}
                       :filter (util/mk-geo-filter geo-map)
                       :sort   (mk-sort sort-map geo-map)
                       :from   (:from page-map)
                       :size   (:size page-map))))))

(defn- filter-by-hours
  [biz-menu-items hours-map]
  (if (nil? hours-map)
    biz-menu-items
    (filter #(util/open-at? hours-map (-> % :_source :business :hours))
            biz-menu-items)))

(def MAX_ITEMS 1000)
(def sort-attributes #{"price" "value" "distance"})

(defn validate-and-search
  ""
  [item-id input-geo-map input-hours-map sort-str input-page-map]

  ;; Validate item-id.
  (if (clojure.string/blank? item-id)
    (responses/error-json {:error "Param 'item_id' must be non-empty."})
      
    ;; Validate location info.
    (let [geo-map (inputs/mk-geo-map input-geo-map)]
      (if (nil? geo-map)
        (validate/response-bad-location input-geo-map)

        ;; Validate sort info.
        (let [sort-map (flurbl/get-sort-map sort-str sort-attributes)]
          (if (nil? sort-map)
            (validate/response-bad-sort sort-str)

            ;; Validate ??? hours.
            (let [hours-map (inputs/get-hours-map input-hours-map)]
              ;; (if (nil? hours-map)
              ;; (validate/response-bad-hours input-hours-map)

              (let [page-map (inputs/mk-page-map input-page-map)
                    
                    ;; Do search, getting lots of (MAX_ITEMS) results.
                    ;; Return *only* the actual hits.
                    ;; But we lose the actual number of results!
                    es-items (get-items item-id geo-map sort-map
                                        {:from 0, :size MAX_ITEMS})

                    ;; Possible post-facto filter using hours-map.
                    items (filter-by-hours es-items hours-map)
                    
                    ;; Gather metadata from the results returned.
                    metadata (meta/get-metadata items)]
                    
                ;; Extract info from ES-results, create JSON response.
                (mk-response items metadata
                             item-id geo-map hours-map sort-map page-map)
                ))))))))
