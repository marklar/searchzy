(ns searchzy.search.business
  (:require [searchzy.search
             [util :as util]]
            [clojurewerkz.elastisch
             [native :as es]
             [query  :as es-q]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(defn -mk-geo-filter
  "Create map for filtering by geographic distance."
  [miles lat lng]
  {:geo_distance {:distance (str miles "mi")
                  :latitude_longitude (str lat "," lng)}})

(defn -mk-sort
  "Create map for sorting results, depending on sort setting."
  [by-value?]
  (if by-value?
    {:value_score_int :desc}
    {:_score :desc}))

(defn -mk-query
  "Create map for querying - either just by 'query',
   or with a scoring fn for sorting."
  [by-value? query]
  (let [q {:term {:name query}}]
    (if by-value?
      ;; Simple query.
      q
      ;; Function_score query, for sorting by value_score_int.
      {:function_score
       {:query q
        ;; Replace the _score with a modified version.
        :boost_mode "replace"
        :script_score {:script "_score + (doc['value_score_int'].value / 20)"}}
       })))

(defn search
  "Perform ES search, return results map.
   If by-value? is true, change scoring function and sort by it.
   types: string string float float float bool int int"
  [query address miles lat lng by-value? from size]
  (let [{lat :lat lng :lng}  (util/get-lat-lng lat lng address)]
    (es-doc/search "businesses" "business"
                   :query  (-mk-query by-value? query)
                   :filter (-mk-geo-filter miles lat lng)
                   :sort   (-mk-sort by-value?)
                   :from   from
                   :size   size)))
