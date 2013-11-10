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
  [results metadata item-id geo-map page-map]
  (let [day-of-week (util/get-day-of-week)
        resp-hits (map #(mk-one-hit % day-of-week (:coords geo-map))
                       (map :_source (:hits results)))]
    (responses/ok-json
     {:endpoint "/v1/business_menu_items"   ; TODO: pass this in
      :arguments {:item_id item-id
                  :geo_filter geo-map
                  :paging page-map
                  :day_of_week day-of-week}
      :results {:count (:total results)
                :prices_micros (:prices-micros metadata)
                :latest_close (:latest-close metadata)
                :hits resp-hits
                }})))

(def idx-name (:index (:business_menu_items cfg/elastic-search-names)))
(def mapping-name (:mapping (:business_menu_items cfg/elastic-search-names)))

(defn- get-results
  "Perform search against item_id."
  [item-id geo-map page-map]
  (:hits
   (es-doc/search idx-name mapping-name
                  :query  {:match {:item_id item-id}}
                  ;; :sort   (array-map :yelp_star_rating  :desc
                  ;;                    :yelp_review_count :desc
                  ;;                    :value_score_picos :desc)
                  :sort   {:value_score_picos :desc}
                  :filter (util/mk-geo-filter geo-map)
                  :from   (:from page-map)
                  :size   (:size page-map))))

(defn- compact [seq] (remove nil? seq))

(defn- get-prices-micros
  [{hits :hits}]
  (let [prices (compact (map #(-> % :_source :price_micros) hits))
        sum (apply + prices)
        cnt (count prices)]
    (if (= 0 cnt)
      {:mean 0, :max 0, :min 0}
      {:mean (/ sum cnt)
       :max  (apply max prices)
       :min  (apply min prices)})))

(defn- get-all-hours-today
  [hits]
  (let [day-of-week (util/get-day-of-week)
        all-hours (compact (map #(-> % :_source :business :hours) hits))]
    (compact (map #(util/get-hours-today % day-of-week) all-hours))))

(def HOUR_MINS 60)

(defn- mins-to-hour
  [minutes]
  (let [m (mod minutes HOUR_MINS)]
    {:hour   (/ (- minutes m) HOUR_MINS)
     :minute m}))

(defn- get-latest-hour
  "Given [{:hour h :minute m}], return the latest one."
  [hour-list]
  (let [as-minutes (fn [{h :hour, m :minute}] (+ m (* HOUR_MINS h)))
        max-minutes (apply max (cons 0 (map as-minutes hour-list)))]
    (mins-to-hour max-minutes)))

(defn- get-latest-close
  "Given ES hits, return a single 'hour' (i.e. {:hour h, :minute m})."
  [{hits :hits}]
  (let [all-closing (map :close (get-all-hours-today hits))]
    (get-latest-hour all-closing)))

(def MAX_ITEMS 1000)

(defn- restrict-hits-in-results
  "Create a restricted set of results to return to client."
  [results {:keys [from size]}]
  (let [new-item-hits (take size (drop from (:hits results)))]
    (assoc results :hits new-item-hits)))

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
          
        (let [page-map (inputs/mk-page-map input-page-map)

              ;; Do search, getting lots of (MAX_ITEMS) results.
              big-item-res (get-results item-id geo-map
                                        {:from 0, :size MAX_ITEMS})

              ;; Gather metadata from the results returned.
              metadata {:prices-micros (get-prices-micros big-item-res)
                        :latest-close (get-latest-close big-item-res)}

              ;; Create a restricted set of results to return to client.
              item-res (restrict-hits-in-results big-item-res page-map)]
            
            ;; Extract info from ES-results, create JSON response.
            (mk-response item-res metadata item-id geo-map page-map))))))
