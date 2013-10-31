(ns searchzy.service.business.search
  (:require [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(defn -mk-geo-filter
  "Create map for filtering by geographic distance."
  [miles lat lon]
  {:geo_distance {:distance (str miles "mi")
                  :latitude_longitude (str lat "," lon)}})

(defn -mk-sort
  "Create map for sorting results, depending on sort setting."
  [by-value?]
  (if by-value?
    {:value_score_int :desc}
    {:_score :desc}))

(defn -mk-function-score-query
  "Return a 'function_score' query-map, for sorting by value_score_int."
  [simple-query-map]
  {:function_score
   {:query simple-query-map
    :boost_mode "replace"   ; Replace _score with the modified one.
    :script_score {:script "_score + (doc['value_score_int'].value / 20)"}}
   })

(defn -mk-query
  "Create map for querying -
   EITHER: just with 'query' -OR- with a scoring fn for sorting."
  [by-value? query]
  (let [simple-query-map {:text {:name query}}]
    (if by-value?
      simple-query-map
      (-mk-function-score-query simple-query-map))))

(defn es-search
  "Perform ES search, return results map.
   If by-value?, change scoring function and sort by its result.
   TYPES: string string float float float bool int int"
  [query miles lat lon sort from size]
  (let [by-value? (= 'value sort)]
    (es-doc/search "businesses" "business"
                   :query  (-mk-query by-value? query)
                   :filter (-mk-geo-filter miles lat lon)
                   :sort   (-mk-sort by-value?)
                   :from   from
                   :size   size)))
