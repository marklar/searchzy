(ns searchzy.service.bmis.items
  "Uses 'items' index to get business_categories and related_items."
  (:require [searchzy.cfg :as cfg]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(def idx-name     (:index   (:items cfg/elastic-search-names)))
(def mapping-name (:mapping (:items cfg/elastic-search-names)))

(defn- biz-cat-id->items
  ":: str -> [Item]"
  [biz-cat-id]
  (let [results (es-doc/search idx-name mapping-name
                               :query {:match {:business_category_ids biz-cat-id}})
        hits (:hits (:hits results))]
    (map (fn [h]
           (let [src (:_source h)]
             {:_id (:_id h), :name (:name src), :fdb_id (:fdb_id src)}))
         hits)))

;;-------------------------

(def MAX_ITEMS_TO_TRY 10)

(defn- item->biz-cat-id
  [es-item]
  (try
    (-> es-item :_source :business_category_ids first)
    (catch Exception e nil)))

(defn get-biz-cat-id
  "Attempts to find a business_category_id by looking in all the returned Items.
   :: str -> str | nil"
  [item-id]
  (let [results (es-doc/search idx-name mapping-name
                               :query {:match {:_id item-id}}
                               :size 1)
        hits (-> results :hits :hits)
        biz-cat-ids (map item->biz-cat-id hits)]
    (or (first (remove nil? biz-cat-ids))
        nil)))

(defn get-related-items
  ":: str -> [Item]"
  [item-id-str]
  (if-let [biz-cat-id (get-biz-cat-id item-id-str)]
    (biz-cat-id->items biz-cat-id)
    []))
