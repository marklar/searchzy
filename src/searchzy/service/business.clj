(ns searchzy.service.business
  (:require [searchzy.service
             [util :as util]
             [inputs :as inputs]
             [validate :as validate]
             [geo :as geo]
             [responses :as responses]
             [query :as q]]
            [searchzy.cfg :as cfg]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

;; -- search --

(defn- mk-sort
  "Create map for sorting results, depending on sort setting."
  [by-value?]
  (if (not by-value?)
    {:_score :desc}
    ;; (array-map :yelp_star_rating  :desc
    ;;            :yelp_review_count :desc
    ;;            :value_score_int   :desc
    ;;            :_score            :desc)))
    {:value_score_int :desc}))

(defn- mk-function-score-query
  "Return a 'function_score' query-map, for sorting by value_score_int."
  [simple-query-map]
  {:function_score
   {:query simple-query-map
    :boost_mode "replace"   ; Replace _score with the modified one.
    :script_score {:script "_score + (doc['value_score_int'].value / 20)"}}
   })

(defn- mk-query
  "Create map for querying -
   EITHER: just with 'query' -OR- with a scoring fn for sorting."
  [by-value? query query-type]
  (let [simple-query-map
        (if (= query-type :prefix)
          (util/mk-suggestion-query query)
          {query-type {:name {:query query
                              :operator "and"}}})]
    (if by-value?
      simple-query-map
      (mk-function-score-query simple-query-map))))

(defn get-results
  "Perform ES search, return results map.
   If by-value?, change scoring function and sort by its result.
   TYPES: string string float float float bool int int"
  [query-str query-type geo-map sort page-map]
  (let [by-value? (= 'value sort)
        es-names (:businesses cfg/elastic-search-names)]
    (:hits (es-doc/search (:index es-names) (:mapping es-names)
                          :query  (mk-query by-value? query-str query-type)
                          :filter (util/mk-geo-filter geo-map)
                          :sort   (mk-sort by-value?)
                          :from   (:from page-map)
                          :size   (:size page-map)))))

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
