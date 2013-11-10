(ns searchzy.service.business.search
  (:require [clojurewerkz.elastisch.native
             [document :as es-doc]]
            [searchzy.service.util :as util]
            [searchzy.cfg :as cfg]))

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

(defn es-search
  "Perform ES search, return results map.
   If by-value?, change scoring function and sort by its result.
   TYPES: string string float float float bool int int"
  [query-str query-type geo-map sort page-map]
  (let [by-value? (= 'value sort)
        es-names (:businesses cfg/elastic-search-names)]
    (es-doc/search (:index es-names) (:mapping es-names)
                   :query  (mk-query by-value? query-str query-type)
                   :filter (util/mk-geo-filter geo-map)
                   :sort   (mk-sort by-value?)
                   :from   (:from page-map)
                   :size   (:size page-map))))
