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
     :name {:type "string"}
     :stemmed_name {:type "string", :analyzer "snowball"}}}})

(defn- mg->es
  [mg-map biz-cat-ids]
  {:business_category_ids biz-cat-ids
   :name (:name mg-map)
   :stemmed_name (:name mg-map)
   :fdb_id (:fdb_id mg-map)})

(defn- add-to-idx
  "Given an ItemCategory mongo-map,
   get embedded :item maps.
   Convert each to es-map and add to index."
  [{:keys [business_category_ids items]}]
  (doseq [i items]
    (if (:is_searchable_ind i)
      (es-doc/put idx-name mapping-name
                  (str (:_id i))   ; explicit ID
                  (mg->es i business_category_ids)))))

(defn recreate-idx
  []
  (util/recreate-idx idx-name mapping-types))

(defn- mg-fetch
  [& {:keys [limit after]}]
  (mg/fetch :item_categories
            :limit limit
            :where (merge
                    {:is_searchable_ind true}
                    (if after
                      {:updated_at {:$gte after}}
                      {}))))

(defn mk-idx
  "Fetch ItemCategories from MongoDB.
   For each ItemCategory, get embedded :items.
   For each Item, add to index.
   Return count (of ItemCategories)."
  [& {:keys [limit after]}]
  (if-not after
    (recreate-idx))
  (doseq-cnt add-to-idx
             10
             (mg-fetch :limit limit :after after)))
