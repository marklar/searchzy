(ns searchzy.index.list
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [searchzy.cfg :as cfg]
            [clojure.string :as str]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))

;; Get CFG info.
(def idx-name     (:index   (:lists cfg/elastic-search-names)))
(def mapping-name (:mapping (:lists cfg/elastic-search-names)))

;; http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping.html
;; Only when the defaults need to be overridden must a mapping
;; definition be provided.
(def mapping-types
  {mapping-name
   {:properties
    {
     ;; Doesn't exist yet.
     :location_id              {:type "string"}
     ;;
     :seo_business_category_id {:type "string"}
     :seo_region_id {:type "string"}
     :seo_item_id   {:type "string"}
     :latitude_longitude  {:type "geo_point"
                           :null_value "66.66,66.66"
                           :include_in_all false}
     :area_type     {:type "string"}
     :state         {:type "string"}
     }}})

;; {:_id #<ObjectId 52f2dea8f1d37f36a6006af4>,
;;  :postal_code nil,
;;  :active_ind true,
;;  :coordinates [-73.98994069999999 40.7412875],
;;  :top_list_permalink wax-flatiron,
;;  :area_permalink flatiron,
;;  :neighborhood_id nil,
;;  :name Flatiron,
;;  :permalink nyc/wax-flatiron,
;;  :city Manhattan,
;;  :seo_item_id #<ObjectId 4fc66d40c9b16860e200003f>,
;;  :state NY,
;;  :legacy_area_top_list_id #<ObjectId 4fc7c4ecc9b168870e00001a>,
;;  :updated_at #inst "2014-02-06T01:00:24.258-00:00",
;;  :area_type neighborhood,
;;  :keyword nil,
;;  :seo_region_id #<ObjectId 4fc6aba5c9b16867c7000001>,
;;  :high_quality_ind false,
;;  :legacy_area_id #<ObjectId 4fb4fb6fc9b16853d3000014>,
;;  :created_at #inst "2014-02-06T01:00:24.258-00:00"}

(defn- coords->str
  [[lon lat]]
  (clojure.string/join "," [(str lat) (str lon)]))

(defn- mg->es
  [mg-map]
  (let [lat-lon (coords->str (:coordinates mg-map))]
    (assoc mg-map :latitude_longitude lat-lon)))
  
(defn- add-to-idx
  "Given a BusinessCategory mongo-map,
   1. convert to es-map, and
   2. add to ES index (explicitly providing _id)."
  [mg-map]
  (es-doc/put idx-name mapping-name
              (str (:_id mg-map))
              (mg->es mg-map)))

(defn- mg-fetch
  ":limit - max number to fetch
   :after - Date; fetch only those whose updated_at is after that"
  [& {:keys [limit after]}]
  (mg/fetch :lists
            :limit limit
            :where (if after
                     {:updated_at {:$gte after}}
                     {})))

(defn recreate-idx []
  (util/recreate-idx idx-name mapping-types))

(defn mk-idx
  "Fetch Lists from MongoDB.
   Add them to ES index.
   Return count.

   Assumes already connected to:
     - a particular MongoDB collection,
     - ElasticSearch
  "
  [& {:keys [limit after]}]
  (if (nil? after)
    (recreate-idx))
  (doseq-cnt add-to-idx 5000 (mg-fetch :limit limit :after after)))
