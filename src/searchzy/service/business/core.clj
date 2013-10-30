(ns searchzy.service.business.core
  (:require [searchzy.service
             [inputs :as inputs]
             [geo :as geo]
             [util :as util]
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
  {:_id id :name n :address a :permalink p :lat_lng l})

(defn -mk-response
  "From ES response, create service response."
  [{hits-map :hits} query miles address lat lng sort from size]
  (util/ok-json-response
   {:query query  ; normalized query, that is
    :index "businesses"  ; TODO - add this to cfg
    :geo_filter {:miles miles :address address :lat lat :lng lng}
    :sort sort
    :paging {:from from :size size}
    :total_hits (:total hits-map)
    :hits (map -mk-hit-response (:hits hits-map))}))

(defn validate-and-search
  [orig-query address miles orig-lat orig-lng sort from size]

  ;; Validate query.
  (let [query (q/normalize orig-query)]
    (if (clojure.string/blank? query)
      (validate/response-bad-query orig-query query)
      
      ;; Validate location info.
      (let [lat (inputs/str-to-val orig-lat nil)
            lng (inputs/str-to-val orig-lng nil)]
        (if (validate/invalid-location? address lat lng)
          (validate/response-bad-location address orig-lat orig-lng)
          
          ;; Validate sort - #{nil 'value 'lexical}.  Def: 'value.
          (let [sort (inputs/str-to-val sort 'value)]
            (if (not (validate/valid-sort? sort))
              (validate/response-bad-sort sort)
              
              ;; OK, make query.
              (let [;; transform params
                    miles  (inputs/str-to-val miles 4.0)
                    from   (inputs/str-to-val from 0)
                    size   (inputs/str-to-val size 10)
                    {lat :lat lng :lng} (geo/get-lat-lng lat lng address)
                    ;; fetch results
                    es-res (search/es-search query miles lat lng sort from size)]

                ;; Extract info from ES-results, create JSON response.
                (-mk-response es-res query miles address lat lng
                              sort from size)))))))))


