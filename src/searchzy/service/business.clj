(ns searchzy.service.business
  (:use [clojure.core.match :only (match)])
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [flurbl :as flurbl]
             [util :as util]
             [inputs :as inputs]
             [responses :as responses]]
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

(defn es-search
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

(defn- filter-by-hours
  [hours-map businesses]
  (if (= {} hours-map)
    businesses
    (filter #(util/open-at? hours-map (-> % :_source :hours))
            businesses)))

(def MAX_ITEMS 1000)
(defn get-results
  "Returns hits -plus- total."
  [{:keys [query geo-map hours-map sort-map page-map]}]
  (if (nil? (:wday hours-map))
    ;; We don't need to post-filter results.
    ;; So we have ES do the paging for us.
    (es-search query :match geo-map sort-map page-map)

    ;; We DO need to post-filter.
    ;; But first let's get lots...
    (let [{hits :hits} (es-search query :match geo-map sort-map
                                  {:from 0, :size MAX_ITEMS})
          ;; ...then post-filter...
          open-hits (filter-by-hours hours-map hits)
          ;; ...and then do our own paging.
          pageful (take (:size page-map)
                        (drop (:from page-map) open-hits))]
      {:total (count open-hits)
       :hits pageful})))


;; -- create response --

(defn- mk-response-hit
  "From ES hit, make service hit."
  [coords day-of-week biz]
  (let [{id :_id {n :name a :address
                  phone_number :phone_number
                  cs :coordinates
                  hs :hours
                  p :permalink
                  yid :yelp_id
                  ysr :yelp_star_rating
                  yrc :yelp_review_count} :_source} biz]
    (let [dist (util/haversine cs coords)
          hours-today (util/get-hours-for-day hs day-of-week)]
      {:_id id :name n :address a :permalink p
       :yelp {:id yid, :star_rating ysr, :review_count yrc}
       :phone_number phone_number
       :distance_in_mi dist
       :coordinates cs
       :hours_today hours-today})))

(defn- mk-response
  "From ES results, create service response.
   We've already done paging; no need to do so now."
  [es-results {:keys [query geo-map hours-map sort-map page-map]}]
  (let [day-of-week (or (:wday hours-map) (util/get-day-of-week))]
    (responses/ok-json
     {:endpoint "/v1/businesses"   ; TODO: pass this in
      :arguments {:query query
                  :geo_filter geo-map
                  :hours_filter (if (= {} hours-map) nil hours-map)
                  :sort sort-map
                  :paging page-map
                  :day_of_week day-of-week}
      :results {:count (:total es-results)
                :hits (map #(mk-response-hit (:coords geo-map) day-of-week %)
                           (:hits es-results))}})))

(def sort-attrs #{"value" "distance" "score"})

(defn validate-and-search
  "input-args: query-string params, aggregated into sub-hashmaps based on meaning.
   1. Validate args and convert them into needed values for searching.
   2. Perform ES search.
   3. Create proper JSON response."
  [input-args]
  (let [[valid-args err] (inputs/business-clean-input input-args sort-attrs)]
    (if err
      ;; Validation error.
      (responses/error-json err)
      ;; Do ES search.
      (let [results (get-results valid-args)]
        ;; Create JSON response.
        (mk-response results valid-args)))))
