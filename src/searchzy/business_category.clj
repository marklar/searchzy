(ns searchzy.business-category
  (:require [searchzy.index :as index]
            [searchzy.util :as util]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))

(def idx-name "business_categories")
(def mapping-name "business_category")

(def mapping-types
  {mapping-name
   {:properties
    {:id    {:type "string", :index "not_analyzed", :include_in_all false}
     :name  {:type "string"}}
    }})

(defn mk-es-map
  "Given a biz-cat mongo-map, create an ElasticSearch map."
  [{id :_id nm :name}]
  {:id id :name nm})

(defn add-to-idx
  "Given a biz-cat mongo-map, convert to es-map and add to index."
  [mg-map]
  (let [es-map (mk-es-map mg-map)]
    (es-doc/create idx-name mapping-name es-map)))

(defn mk-idx
  "Fetch BusinessCategories from MongoDB & add them to index.  Return count."
  []
  (index/recreate-idx idx-name mapping-types)
  (util/doseq-cnt add-to-idx 10
                  (mg/fetch :business_categories :where {:active_ind true})))
