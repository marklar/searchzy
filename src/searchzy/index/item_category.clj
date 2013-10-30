(ns searchzy.index.item-category
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [searchzy.cfg :as cfg]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))

(def idx-name (:item_categories cfg/index-names))
(def mapping-name "item_category")

(def mapping-types
  {mapping-name
   {:properties
    {:name                  {:type "string"}

     ;; Do we need this?
     :business_category_id  {:type "string"
                             :index "not_analyzed"
                             :include_in_all false}
     }}})

(defn -add-to-idx
  "Given an ItemCategory mongo-map, convert to es-map and add to index."
  [mg-map]
  (es-doc/put idx-name
              mapping-name
              (str (:_id mg-map))
              (dissoc mg-map :_id)))

(defn mk-idx
  "Fetch ItemCategories from MongoDB & add them to index.  Return count."
  []
  (util/recreate-idx idx-name mapping-types)
  (doseq-cnt -add-to-idx
             5
             (mg/fetch :item_categories)))
