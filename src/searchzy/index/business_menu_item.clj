(ns searchzy.index.business-menu-item
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [searchzy.index.business :as biz]
            [searchzy.cfg :as cfg]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))


(def idx-name     (:index   (:business_menu_items cfg/elastic-search-names)))
(def mapping-name (:mapping (:business_menu_items cfg/elastic-search-names)))

(def mapping-types
  {mapping-name
   {:properties
    {
     ;; SEARCH
     ;; from the menu_item itself
     :item_id {:type "string" :index "not_analyzed"}

     ;; FILTER
     ;; from the business
     :latitude_longitude {:type "geo_point"
                          :null_value "66.66,66.66"
                          :include_in_all false}

     ;; SORT
     ;; from the business
     :yelp_star_rating  {:type "float"   :null_value 0 :include_in_all false}
     :yelp_review_count {:type "integer" :null_value 0 :include_in_all false}
     ;; from the menu_item itself
     :value_score_picos {:type "long"    :null_value 0 :include_in_all false}
     }}})

;; -- search document --

(defn- mk-es-item-map
  "Given a MenuItem, create a MenuItem-specific es-map.
   Later we'll add in its Business info."
  [item]
  (if (nil? (:item_id item))
    nil
    (let [vsp (:value_score_picos item)]
      {:_id (:_id item)
       :fdb_id (:fdb_id item)
       :name (:name item)
       :item_id (str (:item_id item))
       :price_micros (:price_micros item)
       :value_score_picos vsp})))

(defn- get-items-from-mg-map
  [{{sections :sections} :unified_menu}]
  (flatten (map :items sections)))

(defn- mk-es-maps
  "Given a business mongo-map, return a seq of menu-item-maps."
  ([mg-map]
     (mk-es-maps mg-map (biz/mk-es-map mg-map)))
  ([mg-map biz-map]
     (let [items (get-items-from-mg-map mg-map)]
       (map
        #(assoc %
           :yelp_star_rating   (:yelp_star_rating biz-map)
           :yelp_review_count  (:yelp_review_count biz-map)
           :latitude_longitude (:latitude_longitude biz-map)
           :business biz-map)
        (remove nil? (map mk-es-item-map items))))))

(defn- put [id es-map]
  (es-doc/put idx-name mapping-name id es-map))

(defn- add-es-maps
  [es-maps]
  (doseq [m es-maps]
    (let [id (str (:_id m))]
      (put id m))))

;; -- public --

(defn add-to-idx
  "Given a business mongo-map,
   extract each embedded BusinessUnifiedMenuItems.
   For each BusinessUnifiedMenuItem,
   convert to es-map (along w/ embedded Business information),
   and add the es-map to the index."
  ([mg-map]
     (add-es-maps (mk-es-maps mg-map)))
  ([mg-map biz-es-map]
     (add-es-maps (mk-es-maps mg-map biz-es-map))))

(defn recreate-idx []
  (util/recreate-idx idx-name mapping-types))

;; TODO - this is a copy from business.clj
(defn- file->ids
  ":: str -> [objID]"
  [ids-file]
  (let [biz-id-strs (util/file->lines ids-file)]
    ;; I don't know hy 'str' here is necessary!!!!!
    (map #(mg/object-id (str %)) biz-id-strs)))

;; TODO - this is a copy from business.clj
(defn- query-map
  "Takes filename, datetime.
   Returns a :where clause for Mongo query.
   :: (DateTime, str) -> hash-map"
  [after ids-file]
  (merge (if after
           {:updated_at {:$gte after}}
           {})
         (if ids-file
           {:_id {:$in (file->ids ids-file)}}
           {})))

;; TODO - this is a copy from business.clj
(defn- mg-fetch
  ":limit    - max number to fetch
   :after    - Date; fetch only those whose updated_at is after that
   :ids-file - business IDs in file; fetch only matching"
  [& {:keys [limit after ids-file]}]
  (mg/fetch :businesses
            :limit limit
            :where (query-map after ids-file)))

(defn mk-idx
  "Fetch Businesses from MongoDB.
   Add each (and its embedded BusinessUnifiedMenuItems) to the index.
   Return count (of Businesses)."
  [& {:keys [limit after ids-file]}]
  (if-not ids-file
    (recreate-idx))
  (doseq-cnt add-to-idx
             5000
             (mg-fetch :limit limit :after after :ids-file ids-file)))
