(ns searchzy.index.biz-combined
  (:use [searchzy.util])
  (:require [somnium.congomongo :as mg]
            [searchzy.index
             [util :as util]
             [business :as biz]
             [business-menu-item :as item]]))

(defn- add-to-idx
  ;; In parallel, using agents?
  [mg-map]
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
  [& {:keys [limit after ids-file]}]

  ;; Recreate indices only if starting from scratch, not when updating.
  ;; We know we're updating if we have ids-file.
  (if-not (or after ids-file)
    (do
      (biz/recreate-idx)
      (item/recreate-idx)))

  (doseq-cnt add-to-idx   ; what to do to each Mongo doc
             5000         ; output heartbeat each 5k docs
             ;; fetch Mongo docs
             (biz/mg-fetch :limit limit
                           :after after
                           :ids-file ids-file)))
