(ns searchzy.service.biz-counts
  (:use [clojure.core.match :only (match)])
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [util :as util]
             [geo-util :as geo-util]
             [inputs :as inputs]
             [responses :as responses]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(defn- mk-response
  [valid-args results]
  (let [item-id (:item-id valid-args)]
    {:endpoint "/v1/business_counts"
     :arguments {:item_id item-id
                 :geo_filter (:geo-map valid-args)}
     :results results}))

;;---------------------------

(defn- bmis-query
  [valid-args]
  {:filtered
   {;; There's no query at all.  Only...
    :query {:match_all {}}
    ;; ...a filter.
    :filter {:bool {:must [(geo-util/mk-geo-filter (:geo-map valid-args))
                           {:term {:item_id (:item-id valid-args)}}]}}}})

(def MAX_BMIS 1000)

(defn- search-bmis
  "Query :business_menu_items for the item_id.
   Filter by geo.
   Group the BMIs by business.
   Return count of businesses."
  [valid-args]
  (let [es-names (:business_menu_items cfg/elastic-search-names)
        geo-filter (geo-util/mk-geo-filter (:geo-map valid-args))
        results (es-doc/search (:index es-names)
                               (:mapping es-names)
                               :query (bmis-query valid-args)
                               :size MAX_BMIS)
        bmis (:hits (:hits results))
        biz-ids (map #(-> % :_source :business :_id) bmis)]
    (responses/ok-json (mk-response valid-args
                                    {:count (count (distinct biz-ids))
                                     :business_ids biz-ids}))))

;;---------------------------

(defn- bizs-query
  [geo-map]
  {:filtered
   {;; There's no query at all.  Only...
    :query {:match_all {}}
    ;; ...a filter.
    :filter (geo-util/mk-geo-filter geo-map)}})

(defn- search-bizs
  [geo-map]
  (let [es-names (:businesses cfg/elastic-search-names)
        cnt (:total (:hits (es-doc/search (:index es-names)
                                          (:mapping es-names)
                                          :size MAX_BMIS
                                          :query (bizs-query geo-map))))]
    (responses/ok-json (mk-response {:geo-map geo-map}
                                    {:count cnt}))))

;;---------------------------

(defn- search
  [valid-args]
  (if (empty? (:item-id valid-args))
    (search-bizs (:geo-map valid-args))
    (search-bmis valid-args)))

;; Return the NUMBER of Businesses
;; which have a menu_item WITH A PRICE for that item_id.
;;
;; Each Business has menu_items -- like this:
;;   * "unified_menu" -> "sections"
;;   * each section -> "items"
(defn validate-and-search
  [input-args]
  (util/validate-and-search input-args inputs/biz-counts-clean-input search))
