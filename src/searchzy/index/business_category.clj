(ns searchzy.index.business-category
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [searchzy.cfg :as cfg]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))

;; Get CFG info.
(def idx-name     (:index   (:business_categories cfg/elastic-search-names)))
(def mapping-name (:mapping (:business_categories cfg/elastic-search-names)))

;; http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping.html
;; Only when the defaults need to be overridden must a mapping
;; definition be provided.
(def mapping-types
  {mapping-name
   {:properties
    {:name {:type "string"}}}})


;; An example mg-map...
;;
;; {:display_order 3,
;;  :menu_category "Day Spa",
;;  :name "Day Spa",
;;  :synced_with_centzy_at #inst "2013-11-12T20:12:15.239-00:00",
;;  :business_major_category_id #<ObjectId 526b3fc46bddbbcb28000012>,
;;  :updated_at #inst "2013-11-12T20:12:15.239-00:00",
;;  :item_category_writable_ids
;;  [#<ObjectId 4fb4706cbcd7ac45cf00002b>
;;   #<ObjectId 4fb4706cbcd7ac45cf000031>
;;   #<ObjectId 526b3fd56bddbbcb280000bb>
;;   #<ObjectId 51bc8d728b2867c339000025>
;;   #<ObjectId 526b3fd66bddbbcb280000c3>
;;   #<ObjectId 51bc8d728b2867c33900002f>],
;;  :yelp_category_names ["Day Spas" "Massage"],
;;  :fdb_id "1c8fd5d9-62aa-b8b9-4001-0141c90ac215",
;;  :created_at #inst "2012-05-17T03:28:42.000-00:00",
;;  :is_searchable_ind true,
;;  :_id #<ObjectId 4fb4706abcd7ac45cf00000d>,
;;  :business_ids nil,
;;  :default_item_id #<ObjectId 51bc8d728b2867c339000026>,
;;  :item_category_ids
;;  [#<ObjectId 4fb4706cbcd7ac45cf00002b>
;;   #<ObjectId 4fb4706cbcd7ac45cf000031>
;;   #<ObjectId 526b3fd56bddbbcb280000bb>
;;   #<ObjectId 51bc8d728b2867c339000025>
;;   #<ObjectId 526b3fd66bddbbcb280000c3>
;;   #<ObjectId 51bc8d728b2867c33900002f>],
;;  :business_major_category_display_order 0,
;;  :active_ind true,
;;  :yelp_category_api_names ["spas" "massage"],
;;  :business_major_category_name "Health & Beauty",
;;  :centzy_id #<ObjectId 4f9b1526c9b16835f801befc>}

;; NB: We don't need to index all that info.
;; We could make an 'es-map', a subset of that data,
;; to be stored in ElasticSearch,
;; just as we do with Businesses.

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
  "Fetch BusinessCategories from MongoDB.
   Add them to ES index.
   Return count.

   Assumes already connected to:
     - a particular MongoDB collection,
     - ElasticSearch
  "
  [& {:keys [limit]}]
  (recreate-idx)
  (doseq-cnt add-to-idx 10 (mg-fetch :limit limit)))
