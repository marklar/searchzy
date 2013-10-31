(ns searchzy.service.business.core
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [inputs :as inputs]
             [geo :as geo]
             [responses :as responses]
             [query :as q]]
            [searchzy.service.business
             [validate :as validate]
             [search :as search]]))

;;
;; the aggregate bits of information i need to display are: avg price,
;; min price, max price and latest opening time.
;;
;; that means having prices and hours in the indices.
;;
;; and for the hours, it means having to know what day it is,
;; in order to extract the proper hours.
;;

(defn -mk-hit-response
  "From ES hit, make service hit."
  [{id :_id {n :name a :search_address
             p :permalink l :latitude_longitude} :_source}]
  {:_id id :name n :address a :permalink p :lat_lon l})

(defn -mk-response
  "From ES response, create service response."
  [{hits-map :hits} query miles address lat lon sort from size]
  (responses/ok-json
   {:query      query  ; Normalized query, that is.
    :index      (:businesses cfg/index-names)
    :geo_filter {:miles miles :address address :lat lat :lon lon}
    :sort       sort
    :paging     {:from from :size size}
    :total_hits (:total hits-map)
    :hits       (map -mk-hit-response (:hits hits-map))}))

(defn validate-and-search
  [orig-query address miles orig-lat orig-lon sort from size]

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
                    miles      (inputs/str-to-val miles 4.0)
                    from       (inputs/str-to-val from 0)
                    size       (inputs/str-to-val size 10)
                    {lat :lat
                     lon :lon} (geo/get-lat-lon lat lon address)
                    ;; fetch results
                    es-res (search/es-search query miles lat lon sort from size)]

                ;; Extract info from ES-results, create JSON response.
                (-mk-response es-res query miles address lat lon
                              sort from size)))))))))

