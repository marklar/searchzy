(ns searchzy.index.business-menu-item
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [searchzy.index.business :as biz]
            [searchzy.cfg :as cfg]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))


(def idx-name (:index (:business_menu_items cfg/elastic-search-names)))
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

(defn -mk-es-item-map
  "Given a MenuItem, create a MenuItem-specific es-map.
   Later we'll add in its Business info."
  [{item_id :item_id :as item}]
  (if (nil? item_id)
    nil
    (let [vsp (:value_score_picos item)]
      {:_id (:_id item)
       :name (:name item)
       :item_id (str item_id)
       :price_micros (:price_micros item)
       :value_score_picos vsp})))

(defn -get-items-from-mg-map
  [{{sections :sections} :unified_menu}]
  (flatten (map :items sections)))

(defn -mk-es-maps
  "Given a business mongo-map, return a seq of menu-item-maps."
  [mg-map]
  (let [biz-map (biz/mk-es-map mg-map)
        items (-get-items-from-mg-map mg-map)]
    (map
     #(assoc %
        :yelp_star_rating   (:yelp_star_rating biz-map)
        :yelp_review_count  (:yelp_review_count biz-map)
        :latitude_longitude (:latitude_longitude biz-map)
        :business biz-map)
     (remove nil? (map -mk-es-item-map items)))))

(defn -new-mk-es-maps
  "Given a business mongo-map, return a seq of menu-item-maps."
  [mg-map biz-map]
  (let [items (-get-items-from-mg-map mg-map)]
    (map
     #(assoc %
        :yelp_star_rating   (:yelp_star_rating biz-map)
        :yelp_review_count  (:yelp_review_count biz-map)
        :latitude_longitude (:latitude_longitude biz-map)
        :business biz-map)
     (remove nil? (map -mk-es-item-map items)))))

(defn -put [id es-map]
  (es-doc/put idx-name mapping-name id es-map))

(defn add-to-idx
  "Given a business mongo-map,
   extract each embedded BusinessUnifiedMenuItems.
   For each BusinessUnifiedMenuItem,
   convert to es-map (along w/ embedded Business information),
   and add the es-map to the index."
  [mg-map]
  (let [es-maps (-mk-es-maps mg-map)]
    (doseq [m es-maps]
      (let [id (str (:_id m))]
        (-put id m)))))

(defn new-add-to-idx
  [mg-map biz-es-map]
  (let [es-maps (-new-mk-es-maps mg-map biz-es-map)]
    (doseq [m es-maps]
      (let [id (str (:_id m))]
        (-put id m)))))

(defn recreate-idx []
  (util/recreate-idx idx-name mapping-types))

(defn mk-idx
  "Fetch Businesses from MongoDB.
   Add it (and its embedded BusinessUnifiedMenuItems) to the index.
   Return count (of Businesses)."
  []
  (recreate-idx)
  (doseq-cnt add-to-idx 5000
             (mg/fetch :businesses :where {:active_ind true})))
