(ns searchzy.service.business
  (:use [clojure.core.match :only (match)])
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [flurbl :as flurbl]
             [util :as util]
             [inputs :as inputs]
             [validate :as validate]
             [geo :as geo]
             [responses :as responses]
             [query :as q]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

;; -- search --

(def DEFAULT_SORT {:value_score_int :desc})

(defn mk-sort
  [sort-map geo-map]
  (let [order (:order sort-map)]
    (match (:attribute sort-map)
           "value"    {:value_score_int order}
           "distance" (flurbl/mk-geo-distance-sort-builder (:coords geo-map) order)
           "score"    {:_score order}
           :else      DEFAULT_SORT)))

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
  [query query-type sort-map]
  (let [simple-query-map
        (if (= query-type :prefix)
          (util/mk-suggestion-query query)
          {query-type {:name {:query query
                              :operator "and"}}})]
    (if (= "value" (:attribute sort-map))
      simple-query-map
      (mk-function-score-query simple-query-map))))

(defn get-results
  "Perform ES search, return results map.
   If sort is by 'value', change scoring function and sort by its result."
  [query-str query-type geo-map sort-map page-map]
  (let [search-fn (if (= "distance" (:attribute sort-map))
                    flurbl/distance-sort-search
                    es-doc/search)
        es-names (:businesses cfg/elastic-search-names)]
    (:hits
     (search-fn (:index es-names) (:mapping es-names)
                :query  (mk-query query-str query-type sort-map)
                :filter (util/mk-geo-filter geo-map)
                :sort   (mk-sort sort-map geo-map)
                :from   (:from page-map)
                :size   (:size page-map)))))

(def MAX_ITEMS 1000)

(defn- filter-by-hours
  [businesses hours-map]
  (if (nil? hours-map)
    businesses
    (filter #(util/open-at? hours-map (-> % :_source :hours))
            businesses)))

(defn get-open-results
  "Returns hits plus total."
  [query-str geo-map hours-map sort-map page-map]
  (if (nil? hours-map)

    ;; We don't need to post-filter results.
    (get-results query-str :match geo-map sort-map page-map)

    ;; First get lots.  Then post-filter.
    (let [es-results (get-results query-str :match geo-map sort-map
                                  {:from 0, :size MAX_ITEMS})
          open-hits (filter-by-hours (:hits es-results) hours-map)]
      {:total (count open-hits)
       :hits open-hits})))


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
        hours-today (util/get-hours-for-day hs day-of-week)]
    {:_id id :name n :address a :permalink p
     :yelp {:id yid, :star_rating ysr, :review_count yrc}
     :phone_number phone_number
     :distance_in_mi dist
     :coordinates cs
     :hours_today hours-today}))

(defn- mk-response
  "From ES response, create service response."
  [es-results query-str geo-map hours-map sort-map page-map]
  (let [day-of-week (util/get-day-of-week)
        pageful (take (:size page-map) (drop (:from page-map) (:hits es-results)))]
    (responses/ok-json
     {:endpoint "/v1/businesses"
      :arguments {:query query-str
                  :sort sort-map
                  :paging page-map
                  :geo_filter geo-map
                  :hours_filter hours-map
                  :day_of_week day-of-week}
      :results {:count (:total es-results)
                :hits (map #(mk-response-hit (:coords geo-map) day-of-week %)
                           pageful)}})))

(def sort-attributes #{"value" "distance" "score"})

(defn validate-and-search
  ""
  [input-query input-geo-map input-hours-map sort-str input-page-map]

  ;; Validate query.
  (let [query-str (q/normalize input-query)]
    (if (clojure.string/blank? query-str)
      (validate/response-bad-query input-query query-str)
      
      ;; Validate location info.
      (let [geo-map (inputs/mk-geo-map input-geo-map)]
        (if (nil? geo-map)
          (validate/response-bad-location input-geo-map)
          
          ;; Validate sort info.
          (let [sort-map (flurbl/get-sort-map sort-str sort-attributes)]
            (if (nil? sort-map)
              (validate/response-bad-sort sort-str)

              ;; Validate ??? hours.
              (let [hours-map (inputs/get-hours-map input-hours-map)]
                
                ;; (if (nil? hours-map)
                ;; (validate/response-bad-hours input-hours-map)

                ;; OK, do search.
                (let [page-map (inputs/mk-page-map input-page-map)
                      results (get-open-results query-str geo-map hours-map
                                                sort-map page-map)]

                  ;; Extract info from ES-results, create JSON response.
                  (mk-response results query-str
                               geo-map hours-map sort-map page-map))))))))))
