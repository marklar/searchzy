(ns searchzy.search.business
  (:require [clojurewerkz.elastisch
             [native :as es]
             [query  :as es-q]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(defn bar
  "Really simple query."
  [query]
  (es-doc/search "business_categories" "business_category"
                 :query {:match {:name query}}))

;;
;; TODO: Data structures?:
;;
;; geo-filter: {:miles m, :lat lat, :lng lng}
;;
;; query:
;;     either a simple query, or a function_score.
;;     the former, if sort by :value_score_int.
;;     the latter, otherwise
;;
;; sort:
;;     either {:value_score_int :desc} or the default {:_score :desc}.
;;     again, depends on sort setting.
;;

(defn -mk-geo-filter
  "Create map for filtering by geographic distance."
  [miles lat lng]
  {:geo_distance {:distance (str miles "mi")
                  :latitude_longitude (str lat "," lng)}})

(defn -mk-sort
  "Create map for sorting results."
  [by-value?]
  (if by-value?
    {:value_score_int :desc}
    {:_score :desc}))

(defn -mk-query
  "Create map for querying - either just by 'query', or with a scoring fn."
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
  "Perform ES search with these params.
   If by-value? is true, change scoring function and sort by it."
  [query miles lat lng by-value?]
  (es-doc/search "businesses" "business"
                 :sort   (-mk-sort by-value?)
                 :filter (-mk-geo-filter miles lat lng)
                 :query  (-mk-query by-value? query)))
