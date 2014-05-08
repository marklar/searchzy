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

(defn get-biz-cat-id
  ":: str -> str | nil"
  [item-id]
  (let [results (es-doc/search idx-name mapping-name
                               :query {:match {:_id item-id}}
                               :size 1)]
    (try
      (-> results :hits :hits first :_source :business_category_ids first)
      (catch Exception e nil))))

(defn get-related-items
  ":: str -> [Item]"
  [item-id-str]
  (let [biz-cat-id (get-biz-cat-id item-id-str)]
    (biz-cat-id->items biz-cat-id)))
