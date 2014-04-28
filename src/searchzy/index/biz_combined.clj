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

(defn- mg-fetch
  "If there are biz-ids in ids-file, limit to those.
   Otherwise, just fetch all of them."
  [& {:keys [limit ids-file]}]
  (maybe-take
   limit
   (if ids-file
     ;; FIXME
     (map #(mg/fetch-by-id :businesses (mg/object-id %))
          (util/file->lines ids-file))
     (mg/fetch :businesses))))

(defn mk-idx
  "Fetch Businesses from MongoDB.
   Use each to add to both indices:
     - businesses
     - business_menu_items
   Return count (of Businesses)."
  [& {:keys [limit ids-file]}]

  ;; Recreate indices only if starting from scratch, not when updating.
  ;; We know we're updating if we have ids-file.
  (if (nil? ids-file)
    (biz/recreate-idx)
    (item/recreate-idx))

  (doseq-cnt add-to-idx   ; what to do to each Mongo doc
             5000         ; output heartbeat each 5k docs
             ;; fetch Mongo docs
             (mg-fetch :limit limit :ids-file ids-file)))
