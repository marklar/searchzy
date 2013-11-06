(ns searchzy.index.business-menu-item
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [searchzy.index.business :as biz]
            [searchzy.cfg :as cfg]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))


(def idx-name (:index (:business_menu_items cfg/electric-search-names)))
(def mapping-name (:mapping (:business_menu_items cfg/electric-search-names)))

(def mapping-types
  {mapping-name
   {:properties
    {
     ;; SEARCH
     :item_id {:type "string"
               :index "not_analyzed"}

     ;; FILTER
     :latitude_longitude {:type "geo_point"
                          :null_value "66.66,66.66"
                          :include_in_all false}

     ;; SORT
     :value_score_picos {:type "long"
                         :null_value 0
                         :include_in_all false}
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
        :latitude_longitude (:latitude_longitude biz-map)
        :business biz-map)
     (remove nil? (map -mk-es-item-map items)))))

(defn -add-to-idx
  "Given a business mongo-map,
   extract each embedded BusinessUnifiedMenuItems.
   For each BusinessUnifiedMenuItem,
   convert to es-map (along w/ embedded Business information),
   and add the es-map to the index."
  [mg-map]
  (let [es-maps (-mk-es-maps mg-map)]
    ;;
    ;; TODO
    ;; es-doc/put returns a Clojure map.
    ;; To check if successful, use response/ok? or response/conflict?
    ;; 
    ;; With es-doc/put (vs. es-doc/create), you supply the _id separately.
    ;;
    (doseq [m es-maps]
      (es-doc/put idx-name
                  mapping-name
                  (str (:_id m))
                  m))))

(defn mk-idx
  "Fetch Businesses from MongoDB.
   Add it (and its embedded BusinessUnifiedMenuItems) to the index.
   Return count (of Businesses)."
  []
  (util/recreate-idx idx-name mapping-types)
  (doseq-cnt -add-to-idx 5000
             (mg/fetch :businesses :where {:active_ind true})))
