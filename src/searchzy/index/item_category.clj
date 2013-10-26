(ns searchzy.index.item-category
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))

(def idx-name "item_categories")
(def mapping-name "item_category")

(def mapping-types
  {mapping-name
   {:properties
    {:id                    {:type "string"
                             :index "not_analyzed"
                             :include_in_all false}

     :business_category_id  {:type "string"
                             :index "not_analyzed"
                             :include_in_all false}
                             
     :name                  {:type "string"}
     }}})

(defn add-to-idx
  "Given an ItemCategory mongo-map, convert to es-map and add to index."
  [mg-map]
  (let [es-map (util/rm-leading-underbar mg-map)]
    (es-doc/create idx-name mapping-name es-map)))

(defn mk-idx
  "Fetch ItemCategories from MongoDB & add them to index.  Return count."
  []
  (util/recreate-idx idx-name mapping-types)
  (doseq-cnt add-to-idx 5
                  (mg/fetch :item_categories)))
