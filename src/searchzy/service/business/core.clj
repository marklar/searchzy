(ns searchzy.service.business.core
  (:require [searchzy.service
             [util :as util]
             [inputs :as inputs]
             [geo :as geo]
             [responses :as responses]
             [query :as q]]
            [searchzy.service.business
             [validate :as validate]
             [search :as search]]))

;; -- create response --

(defn- mk-response-hit
  "From ES hit, make service hit."
  [coords day-of-week {id :_id
                          {n :name a :address
                           phone_number :phone_number
                           cs :coordinates
                           hs :hours
                           ysr :yelp_star_rating
                           yrc :yelp_review_count
                           yid :yelp_id
                           p :permalink} :_source}]
  (let [dist (util/haversine cs coords)
        hours-today (util/get-hours-today hs day-of-week)]
    {:_id id :name n :address a :permalink p
     :yelp {:id yid, :star_rating ysr, :review_count yrc}
     :phone_number phone_number
     :distance_in_mi dist
     :coordinates cs
     :hours_today hours-today}))

;;
;; the aggregate info:
;;   * latest closing time
;;   * prices
;;     - mean
;;     - min
;;     - max
;;
(defn- mk-response
  "From ES response, create service response."
  [es-results query-str geo-map sort pager]
  (let [day-of-week (util/get-day-of-week)]
    (responses/ok-json
     {:endpoint "/v1/businesses"
      :arguments {:query query-str
                  :sort sort
                  :paging pager
                  :geo_filter geo-map
                  :day_of_week day-of-week}
      :results {:count (:total es-results)
                :hits (map #(mk-response-hit (:coords geo-map) day-of-week %)
                           (:hits es-results))}})))

;; -- do search --
          
(defn validate-and-search
  ""
  [input-query input-geo-map sort input-page-map]

  ;; Validate query.
  (let [query-str (q/normalize input-query)]
    (if (clojure.string/blank? query-str)
      (validate/response-bad-query input-query query-str)
      
      ;; Validate location info.
      (let [geo-map (inputs/mk-geo-map input-geo-map)]
        (if (nil? geo-map)
          (validate/response-bad-location input-geo-map)
          
          ;; Validate sort - #{nil 'value 'lexical}.  Def: 'value.
          (let [sort (inputs/str-to-val sort 'value)]
            (if (not (validate/valid-sort? sort))
              (validate/response-bad-sort sort)
              
              ;; OK, do search.
              (let [page-map (inputs/mk-page-map input-page-map)
                    es-results (search/get-results query-str :match
                                                   geo-map sort page-map)]

                ;; Extract info from ES-results, create JSON response.
                (mk-response es-results query-str geo-map sort page-map)))))))))
