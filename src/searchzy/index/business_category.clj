(ns searchzy.index.business-category
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))

(def idx-name "business_categories")
(def mapping-name "business_category")

(def mapping-types
  {mapping-name
   {:properties
    {:name  {:type "string"}}
    }})

(defn -add-to-idx
  "Given a biz-cat mongo-map, convert to es-map and add to index."
  [mg-map]
  (es-doc/put idx-name mapping-name
              (str (:_id mg-map))
              (dissoc mg-map :_id)))

(defn mk-idx
  "Fetch BusinessCategories from MongoDB & add them to index.  Return count."
  []
  (util/recreate-idx idx-name mapping-types)
  (doseq-cnt -add-to-idx 10
             (mg/fetch :business_categories
                       :where {:active_ind true})))
