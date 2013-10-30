(ns searchzy.search.business
  (:require [searchzy.search
             [util :as util]
             [query :as q]]
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
  (let [q {:text {:name query}}]
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

(defn -mk-biz-hit-response
  "From ES hit, make service hit."
  [{id :_id
    {n :name a :search_address
     p :permalink l :latitude_longitude} :_source}]
  {:_id id
   :name n
   :address a
   :permalink p
   :lat_lng l
   })

(defn -mk-biz-response
  "From ES response, create service response."
  [{hits-map :hits}]
  (let [n    (:total hits-map)
        hits (:hits hits-map)]
    {:total_hits n
     :hits (map -mk-biz-hit-response hits)}))

(defn es-search
  "Perform ES search, return results map.
   If by-value?, change scoring function and sort by its result.
   TYPES: string string float float float bool int int"
  [query address miles lat lng sort from size]
  (let [{lat :lat lng :lng}  (util/get-lat-lng lat lng address)

        ;; FIXME: We may have more sort opts than just #{'value 'lexical}.
        by-value? (= 'value sort)

        es-response (es-doc/search "businesses" "business"
                                   :query  (-mk-query by-value? query)
                                   :filter (-mk-geo-filter miles lat lng)
                                   :sort   (-mk-sort by-value?)
                                   :from   from
                                   :size   size)]

    ;; TODO: Move this to separate wrapper fn?
    (assoc (-mk-biz-response es-response)
      ;; This is the normalized query.  Should we also include the orig-query?
      :query query
      :index "businesses"
      :geo_filter {:miles miles
               :address address
               :lat lat
               :lng lng}
      :sort sort
      :paging {:from from
               :size size})))

(defn -valid-sort?
  [sym]
  ;; FIXME: We may have more sort opts than just #{'value 'lexical}.
  (contains? #{'value 'lexical} sym))

(defn -invalid-location?
  [address lat lng]
  (and (clojure.string/blank? address)
       (or (nil? lat) (nil? lng))))

(defn validate-and-search
  [orig-query address miles orig-lat orig-lng sort from size]

  ;; Validate query.
  (let [query (q/normalize orig-query)]
    (if (clojure.string/blank? query)
      (util/error-json-response
       {:error "Param 'query' must be non-empty after normalization."
        :orig-query orig-query
        :normalized-query query})
      
      ;; Validate location info.
      (let [lat (util/str-to-val orig-lat nil)
            lng (util/str-to-val orig-lng nil)]
        (if (-invalid-location? address lat lng)
          (util/error-json-response
           {:error "Must provide EITHER address OR both lat & lng."
            :address address
            :lat orig-lat
            :lng orig-lng})
          
          ;; Validate sort -- either nil, "value" (default), or "lexical".
          (let [sort (util/str-to-val sort 'value)]
            (if (not (-valid-sort? sort))
              (util/error-json-response
               {:error "Param 'sort' must be: 'value', 'lexical', or absent."
                :sort sort})
              
              ;; OK, make query.
              (let [;; transform params
                    miles  (util/str-to-val miles 4.0)
                    from   (util/str-to-val from 0)
                    size   (util/str-to-val size 10)
                    ;; fetch results
                    res (es-search query address
                                   miles lat lng sort from size)]
                (util/ok-json-response res)))))))))
