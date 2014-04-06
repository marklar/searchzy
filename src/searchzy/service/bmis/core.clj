(ns searchzy.service.bmis.core
  "A new version of business-menu-items which employs a 'collar'.
   That is, it always performs sort-by-distance searches,
   and then it 'takes' from them until satisfying 'min_results'.
   or simply running out."
  (:use [clojure.core.match :only (match)])
  (:require [searchzy.cfg :as cfg]
            [searchzy.util]
            [searchzy.service
             [geo-sort :as geo-sort]
             [util :as util]
             [inputs :as inputs]]
            [searchzy.service.bmis
             [hits :as hits]
             [businesses :as bizs]
             [business-categories :as biz-cats]
             [metadata :as meta]
             [filter :as filter]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(def idx-name     (:index   (:business_menu_items cfg/elastic-search-names)))
(def mapping-name (:mapping (:business_menu_items cfg/elastic-search-names)))

(defn- es-search
  "Perform search against item_id, specifically using:
     - geo-distance sort  -AND-
     - geo-distance filter"
  [item-id geo-map sort-map page-map]
  (map :_source
       (:hits
        (:hits
         (es-doc/search idx-name mapping-name
                        :query  {:field {:item_id item-id}}
                        :filter (util/mk-geo-filter geo-map)
                        :sort   (geo-sort/mk-geo-distance-sort-builder
                                 (:coords geo-map) :asc)
                        :from   (:from page-map)
                        :size   (:size page-map))))))

(defn- de-dupe
  "If a biz in bizs is already in bmis, then remove from bizs."
  [bizs bmis]
  (let [biz-ids (map #(-> % :business :_id) bmis)]
    (remove #((set biz-ids) (:_id %)) bizs)))

(defn- maybe-add-unpriced
  [include-businesses-without-price bmis item-id fake-geo-map sort-map fake-pager]
  (if include-businesses-without-price
    (let [biz-cat-id (biz-cats/item-id->biz-cat-id item-id)
          bizs (bizs/for-category biz-cat-id fake-geo-map sort-map fake-pager)
          novel-bizs (de-dupe bizs bmis)
          novel-bmis (map bizs/->bmi novel-bizs)]
      (concat bmis novel-bmis))
    bmis))

(def MAX-BMIS 1000)
(defn- get-all-open-bmis
  "Filter by max_miles, if present.  Sort by distance.
   Then in-process, do additional filtering, collaring, and sorting as needed."
  [{:keys [item-id geo-map collar-map hours-map sort-map
           include-businesses-without-price]}]
  ;; Do search, getting lots of (MAX_BMIS) results.
  ;; Return *only* the actual hits, losing the metadata (actual number of results).
  (let [fake-geo-map (assoc geo-map :miles (:max-miles collar-map))
        fake-pager {:from 0, :size MAX-BMIS}
        priced-bmis (es-search item-id fake-geo-map sort-map fake-pager)
        bmis (maybe-add-unpriced include-businesses-without-price
                                  priced-bmis item-id fake-geo-map
                                  sort-map fake-pager)]
    (filter/filter-collar-sort bmis geo-map collar-map hours-map sort-map)))

(defn- get-day-of-week
  [bmis valid-args]
  (util/get-day-of-week
   (:hours-map valid-args)
   (some #(-> % :business :rails_time_zone) bmis)
   (:utc-offset-map valid-args)))

(defn- search
  [valid-args]
  (let [bmis (get-all-open-bmis valid-args)
        day-of-week (get-day-of-week bmis valid-args)
        ;; Gather metadata from the results returned.
        metadata (meta/get-metadata bmis day-of-week)]
    (hits/mk-response bmis metadata day-of-week valid-args)))

;;-------------------------

(defn validate-and-search
  ""
  [input-args]
  (util/validate-and-search input-args inputs/biz-menu-item-clean-input search))
