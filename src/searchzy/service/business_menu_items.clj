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

(defn- mk-one-hit
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

(defn- mk-response
  "From ES response, create service response."
  [{hits :hits} prices close item-id geo-map page-map]
  (let [day-of-week (util/get-day-of-week)
        resp-hits (map #(mk-one-hit % day-of-week (:coords geo-map))
                       (map :_source (:hits hits)))]
    (responses/ok-json
     {:endpoint "/v1/business_menu_items"   ; TODO: pass this in
      :arguments {:item_id item-id
                  :geo_filter geo-map
                  :paging page-map
                  :day_of_week day-of-week}
      :results {:count (:total hits)
                :prices_picos prices
                :latest_close close
                :hits resp-hits
                }})))

(def idx-name (:index (:business_menu_items cfg/elastic-search-names)))
(def mapping-name (:mapping (:business_menu_items cfg/elastic-search-names)))

(defn- item-search
  "Perform search against item_id."
  [item-id geo-map page-map]
  (es-doc/search idx-name mapping-name
                 :query  {:match {:item_id item-id}}
                 ;; :sort   (array-map :yelp_star_rating  :desc
                 ;;                    :yelp_review_count :desc
                 ;;                    :value_score_picos :desc)
                 :sort   {:value_score_picos :desc}
                 :filter (util/mk-geo-filter geo-map)
                 :from   (:from page-map)
                 :size   (:size page-map)))


(defn- get-prices
  [{hits :hits}]
  (let [prices (remove nil? (map #(-> % :_source :price_micros) (:hits hits)))
        sum (apply + prices)
        cnt (count prices)]
    (if (= 0 cnt)
      {:mean 0, :max 0, :min 0}
      {:mean (/ sum cnt)
       :max  (apply max prices)
       :min  (apply min prices)})))
    
(defn- get-latest-close
  "Get: minute, hour"
  [{hits :hits}]
  (let [day-of-week (util/get-day-of-week)
        all-hours (remove nil? (map #(-> % :_source :business :hours) (:hits hits)))
        all-hours-today (remove nil? (map #(util/get-hours-today % day-of-week) all-hours))
        all-closing (map :close all-hours-today)
        as-minutes (fn [time] (+ (:minute time) (* 60 (:hour time))))
        max-mins (apply max (cons 0 (map as-minutes all-closing)))
        minute (mod max-mins 60)
        hours  (/ (- max-mins minute) 60)]
    {:hour hours
     :minute minute}))

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
              all-page-map {:from 0, :size 100}
              ;; Use all-page-map to make the ES query.
              all-item-res (item-search item-id geo-map all-page-map)
              ;; Gather metadata from the results returned.
              prices       (get-prices all-item-res)
              latest-close (get-latest-close all-item-res)

              ;; THIS SURE IS UGLY, BUT IT WORKS.

              ;; Then use page-map to restrict results.
              hits (:hits all-item-res)
              item-hits (:hits hits)
              new-item-hits (take (:size page-map)
                                  (drop (:from page-map) item-hits))
              new-hits (assoc hits :hits new-item-hits)
              item-res (assoc all-item-res :hits new-hits)]
            
            ;; Extract info from ES-results, create JSON response.
            (mk-response item-res prices latest-close
                         item-id geo-map page-map))))))
