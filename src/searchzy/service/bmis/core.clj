(ns searchzy.service.bmis.core
  "Performs two searches, one for BusinessMenuItems and another for Businesses,
   and then it integrates the results.
   Performs filter-by-hours in-process (i.e. not in ElasticSearch)."
  (:use [clojure.core.match :only (match)])
  (:require [searchzy.cfg :as cfg]
            [searchzy.util]
            [searchzy.service
             [geo-sort :as geo-sort]
             [geo-util :as geo-util]
             [util :as util]
             [inputs :as inputs]]
            [searchzy.service.bmis
             [hours :as hours]
             [price :as price]
             [metadata :as meta]
             [items :as items]
             [hits :as hits]
             [businesses :as bizs]
             [filter :as filter]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(def idx-name     (:index   (:business_menu_items cfg/elastic-search-names)))
(def mapping-name (:mapping (:business_menu_items cfg/elastic-search-names)))

(defn- es-search
  "Perform search against item_id, specifically using:
     - geo-distance sort  -AND-
     - geo-distance filter"
  [item-id merch-appt geo-map page-map]
  (let [geo-filter    (geo-util/mk-geo-filter geo-map)
        merch-filter  (util/mk-merch-appt-filter merch-appt)
        filters       (util/compact [geo-filter merch-filter])]
    (map :_source
         (:hits
          (:hits
           (es-doc/search idx-name mapping-name
                          :query {:filtered {:query {:term {:item_id item-id}}
                                             :filter {:and filters}}}
                          :sort   (geo-sort/mk-geo-distance-sort-builder
                                   (:coords geo-map) :asc)
                          :from   (:from page-map)
                          :size   (:size page-map)))))))

(defn- de-dupe
  "If a biz in bizs is already in bmis, then remove from bizs."
  [bizs bmis]
  (let [biz-ids (map #(-> % :business :_id) bmis)]
    (remove #((set biz-ids) (:_id %)) bizs)))

(defn- maybe-add-unpriced
  [include-unpriced bmis item-id merch-appt geo-map fake-pager]
  (if include-unpriced
    (let [biz-cat-id  (items/get-biz-cat-id item-id)
          bizs        (bizs/for-category biz-cat-id merch-appt geo-map fake-pager)
          novel-bizs  (de-dupe bizs bmis)
          novel-bmis  (map bizs/->bmi novel-bizs)]
      ;; TODO: rather than concating (and later sorting),
      ;; we really ought to 'zipper' together the priced-bmis and the unpriced-bizs.
      ;; But we haven't included distances here (that happens in ns:filter).
      (concat bmis novel-bmis))
    bmis))

(def MAX-BMIS 250)
(defn- get-all-open-bmis
  "Filter by miles.  Sort by distance.
   Then in-process, do additional filtering and sorting as needed."
  [{:keys [item-id merchant-appointment-enabled geo-map hours-map sort-map include-unpriced]}]
  ;; Do search, getting lots of (MAX-BMIS) results.
  ;; Return *only* the actual hits, losing the metadata (actual number of results).
  (let [fake-pager {:from 0, :size MAX-BMIS}
        priced-bmis (es-search item-id merchant-appointment-enabled geo-map fake-pager)
        bmis        (maybe-add-unpriced include-unpriced
                                        priced-bmis item-id
                                        merchant-appointment-enabled
                                        geo-map fake-pager)]
    (filter/filter-sort bmis include-unpriced geo-map hours-map sort-map)))

;;-------------------------

(defn- get-day-of-week
  [bmis valid-args]
  (util/get-day-of-week
   (:hours-map valid-args)
   (some #(-> % :business :rails_time_zone) bmis)
   (:utc-offset-map valid-args)))

;;-------------------------

(defn get-metadata
  "For >>>> v2 <<<<<.
   For v1, use `meta/get-metadata`."
  [bmis day-of-week]
  {:price_micros (price/metadata bmis)
   :business_hours (hours/metadata bmis day-of-week)})

(defn- search
  [valid-args meta-fn]
  (let [bmis (get-all-open-bmis valid-args)
        related-items (items/get-related-items (:item-id valid-args))
        ;; Gather metadata from the results returned.
        day-of-week (get-day-of-week bmis valid-args)
        metadata (meta-fn bmis day-of-week)]
    (hits/mk-response bmis related-items metadata day-of-week valid-args)))

;;-------------------------

(defn validate-and-search
  ""
  [version-str input-args]
  (let [meta-fn (if (= version-str "v1") meta/get-metadata get-metadata)]
    (util/validate-and-search input-args
                              inputs/biz-menu-item-clean-input
                              #(search % meta-fn))))
