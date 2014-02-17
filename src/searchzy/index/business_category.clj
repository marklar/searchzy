(ns searchzy.index.business-category
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [searchzy.cfg :as cfg]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))

;; Get CFG info.
(def idx-name     (:index   (:business_categories cfg/elastic-search-names)))
(def mapping-name (:mapping (:business_categories cfg/elastic-search-names)))

(def mapping-types
  {mapping-name
   {:properties
    {:name {:type "string"}}}})

(defn- add-to-idx
  "Given a BusinessCategory mongo-map,
   1. convert to es-map, and
   2. add to ES index (explicitly providing _id)."
  [mg-map]
  (es-doc/put idx-name mapping-name
              (str (:_id mg-map))
              (dissoc mg-map :_id)))

(defn- recreate-idx
  []
  (util/recreate-idx idx-name mapping-types))

(defn- mg-fetch
  [& {:keys [limit]}]
  (maybe-take limit (mg/fetch :business_categories
                              :where {:is_searchable_ind true})))

(defn mk-idx
  [& {:keys [limit]}]
  "Fetch BusinessCategories from MongoDB.
   Add them to ES index.
   Return count.

   Assumes already connected to:
     - a particular MongoDB collection,
     - ElasticSearch
  "
  (recreate-idx)
  (doseq-cnt add-to-idx 10 (mg-fetch :limit limit)))
