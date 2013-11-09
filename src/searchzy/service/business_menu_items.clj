(ns searchzy.service.business-menu-items
  (:import [java.util Calendar GregorianCalendar])
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [util :as util]
             [inputs :as inputs]
             [geo :as geo]
             [responses :as responses]]
            [searchzy.service.business
             [validate :as validate]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(defn -mk-one-hit
  "Replace :hours with :hours_today, using :day_of_week.
   Add :distance_in_mi."
  [source-map day-of-week coords]
  (let [old-biz     (:business source-map)
        hours       (:hours old-biz)
        hours-today (util/get-hours-today hours day-of-week)
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
        (dissoc source-map :latitude_longitude :yelp_star_rating :yelp_review_count)
      :business new-biz)))

;; TODO
(defn -prices-for-hits
  [resp-hits]
  {:mean 0.0
   :minimum 0.0
   :maximum 0.0})

;; TODO
(defn -latest-close
  [resp-hits]
  {:hour 0 :minute 0})

(defn -mk-response
  "From ES response, create service response."
  [{hits :hits} item-id geo-map page-map]
  (let [day-of-week (util/get-day-of-week)
        resp-hits (map #(-mk-one-hit (:_source %) day-of-week (:coords geo-map))
                       (:hits hits))]
    (responses/ok-json
     {:endpoint "/v1/business_menu_items"   ; TODO: pass this in
      :arguments {:item_id item-id
                  :geo_filter geo-map
                  :paging page-map
                  :day_of_week day-of-week}
      :results {:count (:total hits)
                :prices_picos (-prices-for-hits resp-hits)
                :latest_close (-latest-close resp-hits)
                :hits resp-hits
                }})))

(def -idx-name (:index (:business_menu_items cfg/elastic-search-names)))
(def -mapping-name (:mapping (:business_menu_items cfg/elastic-search-names)))

(defn -item-search
  "Perform search against item_id."
  [item-id geo-map page-map]
  (es-doc/search -idx-name -mapping-name
                 :query  {:match {:item_id item-id}}
                 ;; :sort   (array-map :yelp_star_rating  :desc
                 ;;                    :yelp_review_count :desc
                 ;;                    :value_score_picos :desc)
                 :sort   {:value_score_picos :desc}
                 :filter (util/mk-geo-filter geo-map)
                 :from   (:from page-map)
                 :size   (:size page-map)))

(defn validate-and-search
  ""
  [item-id input-geo-map input-page-map]

  ;; Validate item-id.
  (if (clojure.string/blank? item-id)
    (responses/error-json {:error "Param 'item_id' must be non-empty."})
      
    ;; Validate location info.
    (let [geo-map (inputs/mk-geo-map input-geo-map)]
      (if (nil? geo-map)
        (validate/response-bad-location input-geo-map)
          
        ;; OK, do search.
        (let [page-map (inputs/mk-page-map input-page-map)
              item-res (-item-search item-id geo-map page-map)]
            
            ;; Extract info from ES-results, create JSON response.
            (-mk-response item-res item-id geo-map page-map))))))
