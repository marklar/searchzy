(ns searchzy.item-category
  (:require [searchzy.index :as index]
            [searchzy.util :as util]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]
            ))

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

(defn mk-es-map
  "Given a business mongo-map, create an ElasticSearch map."
  [{id :_id bci :business_category_id nm :name}]
  {:id id :business_category_id bci :name nm})

(defn add-to-idx
  "Given a business mongo-map, convert to es-map and add to index."
  [mg-map]
  (let [es-map (mk-es-map mg-map)]
    ;; (println mg-map)
    ;; (println es-map)
    (es-doc/create idx-name mapping-name es-map)))

(defn mk-idx
  "Fetch Businesses from MongoDB and add them to index.  Return count."
  []
  (index/recreate-idx idx-name mapping-types)
  (util/doseq-cnt add-to-idx 5
                  (mg/fetch :item_categories)))
