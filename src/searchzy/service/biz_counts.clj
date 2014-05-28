(ns searchzy.service.biz-counts
  (:use [clojure.core.match :only (match)])
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [util :as util]
             [inputs :as inputs]
             [responses :as responses]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(defn- mk-query
  [valid-args]
  (let [geo-filter (util/mk-geo-filter (:geo-map valid-args))]
    {:filtered {:query {:term {:item_id (:item-id valid-args)}}
                :filter geo-filter}}))

(defn- mk-response
  [valid-args biz-ids]
  (let [item-id (:item-id valid-args)]
    {:endpoint "/v1/business_counts"
     :arguments {:item_id item-id
                 :geo_filter (:geo-map valid-args)}
     :results {:count (count (distinct biz-ids))
               :business_ids biz-ids}}))

(defn- search
  "Query :business_menu_items for the item_id.
   Filter by geo.
   Group the BMIs by business.
   Return count of businesses."
  [valid-args]
  (let [es-names (:business_menu_items cfg/elastic-search-names)
        bmis (:hits (:hits (es-doc/search (:index es-names)
                                          (:mapping es-names)
                                          :query (mk-query valid-args))))
        biz-ids (map #(-> % :_source :business :_id) bmis)]
    (responses/ok-json (mk-response valid-args biz-ids))))

;; Return the NUMBER of Businesses
;; which have a menu_item WITH A PRICE for that item_id.
;;
;; Each Business has menu_items -- like this:
;;   * "unified_menu" -> "sections"
;;   * each section -> "items"
(defn validate-and-search
  [input-args]
  (util/validate-and-search input-args inputs/biz-counts-clean-input search))
