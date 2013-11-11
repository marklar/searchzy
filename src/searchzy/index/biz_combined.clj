(ns searchzy.index.biz-combined
  (:use [searchzy.util])
  (:require [somnium.congomongo :as mg]
            [searchzy.index
             [util :as util]
             [business :as biz]
             [business-menu-item :as item]]))

(defn- add-to-idx
  [mg-map]
  ;; In parallel, using agents?

  ;; (biz/add-to-idx mg-map)
  ;; (item/add-to-idx mg-map))

  ;; Save work: create a biz/es-map first and pass it in to both fns.
  (let [biz-es-map (biz/mk-es-map mg-map)]
    (biz/add-to-idx mg-map biz-es-map)
    (item/add-to-idx mg-map biz-es-map)))

(defn mk-idx
  "Fetch Businesses from MongoDB.
   Use each to add to both indices:
     - businesses
     - business_menu_items
   Return count (of Businesses)."
  []
  (biz/recreate-idx)
  (item/recreate-idx)
  (doseq-cnt add-to-idx 5000
             (mg/fetch :businesses :where {:active_ind true})))
