(ns searchzy.index.item
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [searchzy.cfg :as cfg]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))

(def idx-name     (:index   (:items cfg/elastic-search-names)))
(def mapping-name (:mapping (:items cfg/elastic-search-names)))

(def mapping-types
  {mapping-name
   {:properties
    {:_id  {:type "string"}
     ;; not necessary to search for name.  just store it.
     :name {:type "string"}}}})

(defn- add-to-idx
  "Given an ItemCategory mongo-map,
   get embedded :item maps.
   Convert each to es-map and add to index."
  [{items :items}]
  (doseq [i items]
    (if (:is_searchable_ind i)
      (es-doc/put idx-name mapping-name
                  (str (:_id i))
                  {:name (:name i)}))))

(defn recreate-idx
  []
  (util/recreate-idx idx-name mapping-types))

(defn- mg-fetch
  [& {:keys [limit]}]
  (maybe-take limit (mg/fetch :item_categories
                              :where {:is_searchable_ind true})))

(defn mk-idx
  "Fetch ItemCategories from MongoDB.
   For each ItemCategory, get embedded :items.
   For each Item, add to index.
   Return count (of ItemCategories)."
  [& {:keys [limit]}]
  (recreate-idx)
  (doseq-cnt add-to-idx 10 (mg-fetch :limit limit)))
