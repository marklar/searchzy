(ns searchzy.service.bmis.business-categories
  (:require [somnium.congomongo :as mg]
            [searchzy.util]))

(defn- item-cat->biz-cat-id
  [item-cat]
  (if-let [biz-cat-id (:business_category_id item-cat)]
    biz-cat-id
    (first (:business_category_ids item-cat))))

(defn- get-ids-alist-item-2-biz-cat
  [item-cat]
  (let [item-ids (map :_id (:items item-cat))
        biz-cat-id (item-cat->biz-cat-id item-cat)]
    (mapcat (fn [item-id] [item-id biz-cat-id]) item-ids)))

(defn- compute-id-map
  "Hash-map: Item IDs -> BusinessCategory IDs.
   Get info from MongoDB."
  []
  (searchzy.util/mongo-connect-db! :main)
  (let [all-item-cats (mg/fetch :item_categories)
        alist (mapcat get-ids-alist-item-2-biz-cat all-item-cats)]
    (apply hash-map alist)))

;;
;; Cache a map of Item IDs -> BusinessCategory IDs.
;;

(def id-map)
(defn- get-item-id-2-biz-cat-id
  []
  (defonce id-map (compute-id-map))
  id-map)

;;-----------

(defn item-id->biz-cat-id
  ":: str -> ObjectID
   Given a string for an Item ID (e.g. \"526b3fe76bddbbcb280001b7\"),
   return a BSON ObjectID for the corresponding BusinessCategory."
  [item-id-str]
  (let [id-map (get-item-id-2-biz-cat-id)
        item-id (mg/object-id item-id-str)]
    (get id-map item-id)))
