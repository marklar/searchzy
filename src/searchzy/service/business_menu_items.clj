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
  [source-map day_of_week lat lon]
  (let [old-biz     (:business source-map)
        hours       (:hours old-biz)
        hours-today (:hours (nth hours day_of_week))
        dist        (util/haversine (:coordinates old-biz) {:lat lat :lon lon})
        new-biz     (assoc (dissoc old-biz :hours :latitude_longitude)
                      :hours_today hours-today
                      :distance_in_mi dist)]
    (assoc (dissoc source-map :latitude_longitude) :business new-biz)))

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
  [{hits :hits} item_id miles address lat lon from size]
  (let [day-of-week (util/get-day-of-week)
        resp-hits (map #(-mk-one-hit (:_source %) day-of-week lat lon)
                       (:hits hits))]
    (responses/ok-json
     {:endpoint "/v1/business_menu_items.json"   ; TODO: pass this in
      :query_string {:item_id item_id
                                        ; FIXME: was it address or lat/lon?
                                        ; how about from/size?
                     }   
      :arguments {:item_id item_id
                  :geo_filter {:miles miles :address address :lat lat :lon lon}
                  :paging {:from from :size size}
                  :day_of_week day-of-week}
      :results {:count (:total hits)
                :prices_picos (-prices-for-hits resp-hits)
                :latest_close (-latest-close resp-hits)
                ;; TODO: group items by businesses.
                :hits resp-hits
                }})))

(def -idx-name (:index (:business_menu_items cfg/elastic-search-names)))
(def -mapping-name (:mapping (:business_menu_items cfg/elastic-search-names)))

(defn -item-search
  "Perform search against item_id."
  [item-id miles lat lon from size]
  (es-doc/search -idx-name -mapping-name
                 :query {:match {:item_id item-id}}
                 :sort  {:value_score_picos :desc}
                 :from  from
                 :size  size))

(defn validate-and-search
  ""
  [item-id address orig-lat orig-lon from size]

  ;; Validate item-id.
  (if (clojure.string/blank? item-id)
    (responses/error-json {:error "Param 'item_id' must be non-empty."})
      
    ;; Validate location info.
    (let [lat (inputs/str-to-val orig-lat nil)
          lon (inputs/str-to-val orig-lon nil)]
      (if (validate/invalid-location? address lat lon)
        (validate/response-bad-location address orig-lat orig-lon)
          
        ;; OK, make query.
        (let [;; transform params
              miles 4.0
              from       (inputs/str-to-val from 0)
              size       (inputs/str-to-val size 10)
              {lat :lat
               lon :lon} (geo/get-lat-lon lat lon address)
              ;; fetch results
              item-res (-item-search item-id miles lat lon from size)]
            
            ;; Extract info from ES-results, create JSON response.
            (-mk-response item-res item-id miles address lat lon from size))))))
