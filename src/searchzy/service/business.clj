(ns searchzy.service.business
  (:use [clojure.core.match :only (match)])
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [geo-sort :as geo-sort]
             [util :as util]
             [geo-util :as geo-util]
             [inputs :as inputs]
             [responses :as responses]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

;; -- search --

(defn- value-sort
  [order]
  (array-map :yelp_star_rating  order
             :yelp_review_count order
             :value_score_int order))

(def DEFAULT_SORT (value-sort :desc))

(defn- mk-sort
  [sort-map geo-map]
  (let [order (:order sort-map)]
    (match (:attribute sort-map)
           "value"    (value-sort order)
           "distance" (geo-sort/mk-geo-distance-sort-builder
                       (:coords geo-map) order)
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

(defn- mk-simple-query-map
  [query query-type]
  (if (= query-type :prefix)
    (util/mk-suggestion-query query)
    {query-type {:name {:query query
                        :operator "and"}}}))

(defn- mk-query
  "Create map for querying -
   EITHER: just with 'query' -OR- with a scoring fn for sorting."
  [query-str query-type sort-map]
  (if (clojure.string/blank? query-str)
    nil
    (let [simple-query-map (mk-simple-query-map query-str query-type)]
      (if (= "value" (:attribute sort-map))
        simple-query-map
        (mk-function-score-query simple-query-map)))))

(defn- mk-biz-cat-id-filter
  [biz-cat-ids]
  (if (empty? biz-cat-ids)
    nil
    {:term {:business_category_ids biz-cat-ids}}))

(defn- mk-filtered-query
  [query-str query-type biz-cat-ids sort-map]
  (let [query-map (mk-query query-str query-type sort-map)
        id-filter (mk-biz-cat-id-filter biz-cat-ids)]
    {:filtered {:query query-map :filter id-filter}}))

(defn es-search
  "Perform ES search, return results map.
   If sort is by 'value', change scoring function and sort by its result."
  [query-str query-type biz-cat-ids geo-map sort-map page-map]
  (let [es-names (:businesses cfg/elastic-search-names)]
    (:hits
     (es-doc/search (:index es-names)
                    (:mapping es-names)
                    :query  (mk-filtered-query query-str query-type
                                               biz-cat-ids sort-map)
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
  [{:keys [query business-category-ids geo-map hours-map sort-map page-map]}]
  (if (nil? (:wday hours-map))

    ;;-- ElasticSearch filters. --
    ;; We don't need to post-filter results based on hours.
    ;; So we can have ES do the paging for us.
    (es-search query :match business-category-ids geo-map sort-map page-map)

    ;;-- We filter. --
    ;; We DO need to post-filter.
    ;; But first let's get lots...
    (let [{hits :hits} (es-search query :match business-category-ids
                                  geo-map sort-map
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
                  bcis :business_category_ids
                  yid :yelp_id
                  ysr :yelp_star_rating
                  yrc :yelp_review_count} :_source} biz]
    (let [dist (geo-util/haversine cs coords)
          hours-today (util/get-hours-for-day hs day-of-week)]
      {:_id id :name n :address a :permalink p
       :business_category_ids bcis
       :yelp {:id yid, :star_rating ysr, :review_count yrc}
       :phone_number phone_number
       :distance_in_mi dist
       :coordinates cs
       :hours_today hours-today})))

(defn- mk-response
  "From ES results, create service response.
   We've already done paging; no need to do so now."
  [es-results {:keys [query business-category-ids
                      geo-map hours-map utc-offset-map
                      sort-map page-map]}]
  (let [rails-time-zone
        (some #(-> % :_source :rails_time_zone) (:hits es-results))
        day-of-week
        (util/get-day-of-week hours-map rails-time-zone utc-offset-map)]
    (responses/ok-json
     {:endpoint "/v1/businesses"   ; TODO: pass this in
      :arguments {:query query
                  :business_category_ids business-category-ids
                  :geo_filter geo-map
                  :hours_filter hours-map
                  :utc_offset utc-offset-map
                  :day_of_week day-of-week
                  :sort sort-map
                  :paging page-map}
      :results {:count (:total es-results)
                :hits (map #(mk-response-hit (:coords geo-map) day-of-week %)
                           (:hits es-results))}})))

(defn- search
  [valid-args]
  (let [results (get-results valid-args)]
    (mk-response results valid-args)))

(defn validate-and-search
  "input-args: HTTP params, aggregated into sub-hashmaps based on meaning.
   1. Validate args and convert them into needed values for searching.
   2. Perform ES search.
   3. Create proper JSON response."
  [input-args]
  (util/validate-and-search input-args inputs/business-clean-input search))
