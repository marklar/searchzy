(ns searchzy.service.suggestions
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [inputs :as inputs]
             [geo :as geo]
             [responses :as responses]
             [query :as q]]
            [searchzy.service.business
             [validate :as validate]
             [search :as biz-search]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))


(defn -mk-biz-hit-response
  "From ES biz hit, make service hit."
  [{id :_id {n :name a :address} :_source}]
  {:id id :name n :address a})

(defn -mk-simple-hit-response
  "For ES hit of either biz-category or item, make a service hit."
  [{i :_id
    {n :name} :_source}]
  {:id i :name n})

(defn -mk-res-map
  [f hits-map]
  {:count (:total hits-map)
   :hits (map f (:hits hits-map))})

(defn -mk-response
  "From ES response, create service response."
  [{biz-hits-map :hits}
   {biz-cat-hits-map :hits}
   {item-hits-map :hits}
   query miles address lat lon]
  (responses/ok-json
   {:endpoint "/v1/suggestions.json"   ; TODO: pass this in
    :query_string {}  ; TODO
    :arguments {:query query
                :geo_filter {:miles miles :address address :lat lat :lon lon}}
    :results {:businesses
              (-mk-res-map -mk-biz-hit-response    biz-hits-map)
              :business_categories
              (-mk-res-map -mk-simple-hit-response biz-cat-hits-map)
              :items
              (-mk-res-map -mk-simple-hit-response item-hits-map)}}))

(defn -simple-search
  "Perform prefix search against names."
  [domain query from size]
  (let [es-names (domain cfg/elastic-search-names)]
    (es-doc/search (:index es-names) (:mapping es-names)
                   :query  {:prefix {:name query}}
                   :from   from
                   :size   size)))

(defn validate-and-search
  "Perform 3 searches (in parallel!):
      - businesses (w/ filtering)
      - business_categories
      - items"
  [orig-query address orig-lat orig-lon]

  ;; Validate query.
  (let [query (q/normalize orig-query)]
    (if (clojure.string/blank? query)
      (validate/response-bad-query orig-query query)
      
      ;; Validate location info.
      (let [lat (inputs/str-to-val orig-lat nil)
            lon (inputs/str-to-val orig-lon nil)]
        (if (validate/invalid-location? address lat lon)
          (validate/response-bad-location address orig-lat orig-lon)
          
          ;; OK, make queries.
          (let [
                ;; transform params
                miles 4.0
                from  0
                size  10
                {lat :lat lon :lon} (geo/get-lat-lon lat lon address)

                ;; fetch results
                ;; TODO: in *parallel*.  How?
                ;;  - pmap: Probably not worth the coordination overhead.
                ;;  - clojure.core.reducers/map
                ;;  - agents (uncoordinated, asynchronous)
                biz-res     (biz-search/es-search query :prefix
                                                  miles lat lon
                                                  nil  ; -sort-
                                                  from size)
                biz-cat-res (-simple-search :business_categories query from size)
                item-res    (-simple-search :items query from size)]
            
            ;; Extract info from ES-results, create JSON response.
            (-mk-response biz-res biz-cat-res item-res
                          query miles address lat lon)))))))
