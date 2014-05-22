(ns searchzy.index.business
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [searchzy.cfg :as cfg]
            [clojure.string :as str]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))


(def idx-name     (:index   (:businesses cfg/elastic-search-names)))
(def mapping-name (:mapping (:businesses cfg/elastic-search-names)))

(def mapping-types
  {mapping-name
   {:properties
    {
     ;; SEARCH
     :name         {:type "string"}
     :phone_number {:type "string"}

     ;; FILTER
     :latitude_longitude {:type "geo_point"
                          :null_value "66.66,66.66"  ; Doesn't seem to work.
                          :include_in_all false}

     ;; An *array* of strings.
     :business_category_ids {:type "string"}

     ;; SORT
     :yelp_star_rating  {:type "float"   :null_value 0 :include_in_all false}
     :yelp_review_count {:type "integer" :null_value 0 :include_in_all false}
     ;; calculated from prices of embedded BusinessItems
     :value_score_int   {:type "integer" :null_value 0 :include_in_all false}
     }}})

;; -- search --

(defn- get-phone-number
  ""
  [{pc :phone_country_code pa :phone_area_code pn :phone_number}]
  (str/join "-" (remove str/blank? [pc pa pn])))

;; -- sort --

(defn- get-value-score
  "For seq of business_item maps, find highest value_score (as % int)."
  [biz-items]
  (let [scores (filter number? (map :value_score biz-items))]
    (int (* 100 (apply max 0 scores)))))

;; -- filter --

(defn- get-lat-lon-str
  "From MongoMap, get two nums, in REVERSE ORDER, and create string.
   e.g. [40.1 10.3] => '10.3,40.1' "
  ;; In MongoDB, the coords are stored 'backwards' (i.e. first lon, then lat).
  [{coords :coordinates}]
  (if (empty? coords)
    "66.66,66.66"
    (str (coords 1) "," (coords 0))))

;; -- presentation --

(defn- get-coords
  "From MongoMap, extract coords."
  [{coords :coordinates}]
  (if (empty? coords)
    nil
    {:lat (coords 1) :lon (coords 0)}))

(defn- get-biz-hour-info
  "From MongoMap's hours info for a single day, extract hours."
  [{d :wday c? :is_closed
    oh :open_hour om :open_minute
    ch :close_hour cm :close_minute}]
  (merge {:wday d}
    (if c?
      {:is_closed true}
      {:hours {:open  {:hour oh :minute om}
               :close {:hour ch :minute cm}}})))

(defn- get-street
  ":: mg-map -> str"
  [{a1 :address_1, a2 :address_2}]
  (str/join ", " (remove str/blank? [a1 a2])))

(defn- get-address
  ":: mg-map -> addr-map"
  [mg-map]
  (assoc
    (select-keys mg-map [:city :state :zip])
    :street (get-street mg-map)))

;; -- search document --

;; PUBLIC -- because used by business-menu-item/-mk-es-maps.
;; FIXME: Change this from using:
;;    embedded :business_items to
;;    :unified_menu => :sections => :items => :value_score_picos
(defn mk-es-map
  "Given a business mongo-map, create an ElasticSearch map."
  [mg-map]
  (assoc
      (select-keys mg-map [:_id :fdb_id :name :permalink
                           :yelp_star_rating :yelp_review_count :yelp_id
                           :rails_time_zone])
    ;; search
    :phone_number (get-phone-number mg-map)
    ;; filter
    :latitude_longitude (get-lat-lon-str mg-map)
    :business_category_ids (map str (:business_category_ids mg-map))
    ;; sort
    :value_score_int (get-value-score (:business_items mg-map))
    ;; presentation
    :address (get-address mg-map)
    :coordinates (get-coords mg-map)
    :hours (map get-biz-hour-info (:business_hours mg-map))
    ))
   
(defn- put [id es-map]
  ;; TODO
  ;; es-doc/put returns a Clojure map.
  ;; To check if successful, use response/ok? or response/conflict?
  ;; With es-doc/put (vs. es-doc/create), you supply the _id separately.
  (es-doc/put idx-name mapping-name id es-map))
  ;; (es-doc/async-put idx-name mapping-name id es-map))

;; TODO: use multimethod here to distinguish between
;; mg-map and es-map.  (How?  Add tag?)
(defn add-to-idx
  "Given a business mongo-map, convert to es-map and add to index."
  ([mg-map]
     (let [es-map (mk-es-map mg-map)]
       (add-to-idx mg-map es-map)))
  ([mg-map es-map]
     (let [id (str (:_id mg-map))]
       (put id (dissoc es-map :_id)))))

;; {:description nil,
;;  :_id #<ObjectId 4fb687b4c9b1687205002954>,
;;  :active_ind false,
;;  :phone_number "3831515",
;;  :coordinates nil,
;;  :address_2 nil,
;;  :yelp_star_rating nil,
;;  :name "Fade 2 Famous",
;;  :permalink "fade-2-famous",
;;  :address_1 nil,
;;  :city nil,
;;  :value_score nil,
;;  :state nil,
;;  :updated_at #inst "2012-05-18T17:32:36.000-00:00",
;;  :zip nil,
;;  :phone_area_code "718",
;;  :seo_only_ind true,
;;  :url nil,
;;  :phone_country_code nil,
;;  :yelp_id nil,
;;  :business_category_ids [],
;;  :country nil,
;;  :created_at #inst "2012-05-18T17:32:36.000-00:00",
;;  :yelp_review_count nil,
;;  :citygrid_listing_id nil}


(defn- mk-query-map
  "Takes filename, datetime.
   Returns a :where clause for Mongo query.
   :: (DateTime, str) -> hash-map"
  [after biz-ids]
  (merge
   ;; Don't include Businesses lacking :address_1.
   {:$and [{:address_1 {:$ne nil}}
           {:address_1 {:$ne ""}}]}
   ;; If user provides a date filter, get only newer stuff.
   (if after
     {:updated_at {:$gte after}}
     {})
   ;; If user specifies particular businesses, get only those.
   (if (empty? biz-ids)
     {}
     {:_id {:$in biz-ids}})))
  
(defn- mk-fetch-opts
  [limit after biz-ids]
  (let [query-map (mk-query-map after biz-ids)
        opts (if (empty? query-map)
               []
               [:where query-map])]
    (if limit
      (concat [:limit limit] opts)
      opts)))

(defn mg-fetch
  ":limit    - max number to fetch
   :after    - Date; fetch only those whose updated_at is after that
   :biz-ids  - business IDs (from file); fetch only matching"
  [& {:keys [limit after biz-ids]}]
  (apply mg/fetch :businesses (mk-fetch-opts limit after biz-ids)))

(defn recreate-idx []
  (util/recreate-idx idx-name mapping-types))

(defn mk-idx
  "Fetch Businesses from MongoDB and add them to index.  Return count."
  [& {:keys [limit after biz-ids]}]
  (if-not (or after biz-ids)
    (recreate-idx))
  (doseq-cnt add-to-idx
             5000
             (mg-fetch :limit limit
                       :after after
                       :biz-ids biz-ids)))
