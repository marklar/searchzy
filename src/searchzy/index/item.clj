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

(defn- get-biz-cat-ids
  [item]
  (let [item-category (mg/fetch-by-id :item_categories
                                      (:item_category_id item))]
    (if (nil? item-category)
      nil    ;; This shouldn't happen, right?
      (:business_category_ids item-category))))

(defn- add-to-idx
  "Given an Item mongo-map,
   first get its business_category_ids via its item_category_id.
   Then convert to es-map and add to index."
  [item]
  (if (:is_searchable_ind item)
    (es-doc/put idx-name mapping-name
                (str (:_id item))   ; explicit ID
                (mg->es item (get-biz-cat-ids item)))))

(defn recreate-idx
  []
  (util/recreate-idx idx-name mapping-types))

(defn- mg-fetch
  [& {:keys [limit after]}]
  (mg/fetch :items
            :limit limit
            :where (merge
                    {:is_searchable_ind true}
                    (if after
                      {:updated_at {:$gte after}}
                      {}))))

(defn mk-idx
  "Fetch Items from MongoDB.
   For each, add to index.
   Return count (of ItemCategories)."
  [& {:keys [limit after]}]
  (if (nil? after)
    (recreate-idx))
  (doseq-cnt add-to-idx
             10
             (mg-fetch :limit limit :after after)))
