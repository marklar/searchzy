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

;;
;; the aggregate info:
;;   * latest closing time
;;   * prices
;;     - mean
;;     - min
;;     - max
;;

(defn -mk-hit-response
  "From ES hit, make service hit."
  [geo-point day-of-week {id :_id
                          {n :name a :address
                           phone_number :phone_number
                           cs :coordinates
                           hs :hours
                           ysr :yelp_star_rating
                           yrc :yelp_review_count
                           yid :yelp_id
                           p :permalink} :_source}]
  (let [dist (util/haversine cs geo-point)
        hours-today (util/get-hours-today hs day-of-week)]
    {:_id id :name n :address a :permalink p
     :yelp {:id yid, :star_rating ysr, :review_count yrc}
     :phone_number phone_number
     :distance_in_mi dist
     :coordinates cs
     :hours_today hours-today}))

(defn -mk-response
  "From ES response, create service response."
  [{hits-map :hits} query miles address lat lon sort from size]
  (let [day-of-week (util/get-day-of-week)]
    (responses/ok-json
     {:endpoint "/v1/businesses"
      :arguments {:query query
                  :sort sort
                  :paging {:from from :size size}
                  :geo_filter {:miles miles :address address :lat lat :lon lon}
                  :day_of_week day-of-week
                  }
      :results {:count (:total hits-map)
                :hits (map #(-mk-hit-response {:lat lat :lon lon} day-of-week %)
                           (:hits hits-map))}})))

(defn validate-and-search
  ;; [orig-query address miles orig-lat orig-lon sort from size]
  [orig-query address orig-lat orig-lon sort from size]

  ;; Validate query.
  (let [query (q/normalize orig-query)]
    (if (clojure.string/blank? query)
      (validate/response-bad-query orig-query query)
      
      ;; Validate location info.
      (let [lat (inputs/str-to-val orig-lat nil)
            lon (inputs/str-to-val orig-lon nil)]
        (if (validate/invalid-location? address lat lon)
          (validate/response-bad-location address orig-lat orig-lon)
          
          ;; Validate sort - #{nil 'value 'lexical}.  Def: 'value.
          (let [sort (inputs/str-to-val sort 'value)]
            (if (not (validate/valid-sort? sort))
              (validate/response-bad-sort sort)
              
              ;; OK, make query.
              (let [;; transform params
                    ;; miles      (inputs/str-to-val miles 4.0)
                    miles      4.0
                    from       (inputs/str-to-val from 0)
                    size       (inputs/str-to-val size 10)
                    {lat :lat lon :lon} (geo/get-lat-lon lat lon address)
                    ;; fetch results
                    es-res (search/es-search query :text
                                             miles lat lon
                                             sort from size)]

                ;; Extract info from ES-results, create JSON response.
                (-mk-response es-res query miles address lat lon
                              sort from size)))))))))
