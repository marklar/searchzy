(ns searchzy.service.mean-prices
  (:use [clojure.core.match :only (match)])
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [util :as util]
             [inputs :as inputs]
             [responses :as responses]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(defn- get-location
  [permalink]
  (let [es-names (:locations cfg/elastic-search-names)]
    (es-doc/search (:index es-names)
                   (:mapping es-names)
                   :query {:term {:permalink permalink}})))

(defn- get-results
  [{:keys [permalink item-ids miles]}]
  ;; for now, return locations
  (get-location permalink))
  ;; from locations, get 

;; for now, just show locations
(defn- mk-response
  [locations {:keys [permalink item-category-ids miles]}]
  (responses/ok-json
   {:endpoint "/v1/mean_prices"  ; todo: pass this in
    :arguments {:permalink permalink
                :item_category_ids item-category-ids
                :miles miles}
    :results (:hits (:hits locations))}))

(defn- search
  [valid-args]
  (let [results (get-results valid-args)]
    (mk-response results valid-args)))

(defn validate-and-search
  "input-args: HTTP params :item-ids, :location-ids, and :miles.
   1. Validate args and convert them into needed values for searching.
   2. Perform ES search.
   3. Create proper JSON response."
  [input-args]
  (util/validate-and-search input-args inputs/mean-prices-input search))
