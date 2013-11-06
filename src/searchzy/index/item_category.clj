(ns searchzy.index.item-category
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [searchzy.cfg :as cfg]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))

;; TODO: merge this with business-category.

(def idx-name (:index (:item_categories cfg/elastic-search-names)))
(def mapping-name (:mapping (:item_categories cfg/elastic-search-names)))

(def mapping-types
  {mapping-name
   {:properties
    {:name {:type "string"}}}})

(defn -add-to-idx
  "Given an ItemCategory mongo-map, convert to es-map and add to index."
  [mg-map]
  (es-doc/put idx-name mapping-name
              (str (:_id mg-map))
              (dissoc mg-map :_id)))

(defn mk-idx
  "Fetch ItemCategories from MongoDB & add them to index.  Return count."
  []
  (util/recreate-idx idx-name mapping-types)
  (doseq-cnt -add-to-idx 10
             (mg/fetch :item_categories)))
