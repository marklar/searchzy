(ns searchzy.service.lists
  "Some useful comment here."
  (:use [clojure.core.match :only (match)])
  (:require [searchzy.cfg :as cfg]
            [searchzy.util]
            [searchzy.service
             [responses :as responses]
             [geo-sort :as geo-sort]
             [util :as util]
             [inputs :as inputs]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(defn- mk-response-hit
  [hit]
  (select-keys (:_source hit)
               [:_id :permalink
                :location_id
                :seo_item_id :seo_business_category_id
                :seo_region_id :coordinates :area_type
                :name :state :postal_code]))

(defn- mk-response
  "From ES results, create service response.
   We've already done paging; no need to do so now."
  [es-results {:keys [location-id seo-business-category-id
                      seo-region-id seo-item-id
                      area-type state
                      geo-map page-map]}]
  (responses/ok-json
   {
    ;; TODO: pass this in
    :endpoint "/v1/lists"

    ;; For 'area-type' and 'state', do we _search_ or do we _filter_?
    :arguments {:attributes {:location_id location-id
                             :seo_business_category_id seo-business-category-id
                             :seo_region_id seo-region-id
                             :seo_item_id seo-item-id
                             :area_type area-type
                             :state state}
                :geo_filter geo-map
                :paging page-map}
    :results {:count (:total (:hits es-results))
              :hits (map mk-response-hit (:hits (:hits es-results)))
              }}))

;;-------

(def idx-name     (:index   (:lists cfg/elastic-search-names)))
(def mapping-name (:mapping (:lists cfg/elastic-search-names)))

(defn- mk-bool-term-query
  [args]
  (let [f (fn [k1 k2]
            (let [val (get args k2)]
              (if (clojure.string/blank? val)
                nil
                {:term {k1 (clojure.string/lower-case val)}})))
        exprs (remove nil? [(f :location_id   :location-id)
                            (f :seo_business_category_id :seo-business-category-id)
                            (f :seo_region_id :seo-region-id)
                            (f :seo_item_id   :seo-item-id  )
                            (f :area_type     :area-type    )
                            (f :state         :state        )])]
    (if (empty? exprs)
      nil
      {:bool {:must exprs}})))

(defn- get-results
  "Perform search, specifically using:
     - geo-distance sort  -AND-
     - geo-distance filter"
  [args]
  (let [geo-map (:geo-map args)
        page-map (:page-map args)]
    (es-doc/search idx-name mapping-name
                   :query {:filtered 
                           {:query (mk-bool-term-query args)
                            :filter (util/mk-geo-filter geo-map)}}
                   :sort   (geo-sort/mk-geo-distance-sort-builder
                            (:coords geo-map) :asc)
                   :from   (:from page-map)
                   :size   (:size page-map))))

;;-------

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
  (util/validate-and-search input-args inputs/lists-clean-input search))
