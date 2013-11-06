(ns searchzy.index.business
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [searchzy.cfg :as cfg]
            [clojure.string :as str]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))

(def idx-name (:index (:businesses cfg/elastic-search-names)))
(def mapping-name (:mapping (:businesses cfg/elastic-search-names)))

;; Each index may have one OR MORE mapping-types (each w/ a diff mapping-name),
;; if you put more than one type of document in your index.
;; I don't think we want to do that.
(def mapping-types
  {mapping-name
   {:properties
    {
     ;; SEARCH
     :name         {:type "string"}
     :phone_number {:type "string"}

     ;; FILTER
     :latitude_longitude {:type "geo_point"
                          :null_value "66.66,66.66"
                          :include_in_all false}

     ;; SORT    (calculated from prices of embedded BusinessItems)
     :value_score_int {:type "integer" :null_value 0 :include_in_all false}

     ;; PRESENTATION - include these in the search document, but don't map them.
     ;; :address     {:index "not_analyzed" :include_in_all false}
     ;; :coordinates {:index "not_analyzed" :include_in_all false}
     ;; :hours       {:index "not_analyzed" :include_in_all false}
     ;; :permalink   {:index "not_analyzed" :include_in_all false}
     }}})

;; -- search --

(defn -get-phone-number
  ""
  [{pc :phone_country_code pa :phone_area_code pn :phone_number}]
  (str/join "-" (remove str/blank? [pc pa pn])))

;; -- sort --

(defn -get-value-score
  "For seq of business_item maps, find highest value_score (as % int)."
  [biz-items]
  (let [scores (filter number? (map :value_score biz-items))]
    (int (* 100 (apply max (cons 0 scores))))))

;; -- filter --

(defn -get-lat-lon-str
  "From MongoMap, get two nums, in REVERSE ORDER, and create string.
   e.g. [10.3 40.1] => '10.3,40.1' "
  ;; In MongoDB, the coords are stored backwards (i.e. first lon, then lat).
  [{coords :coordinates}]
  (if (empty? coords)
    nil
    (str (coords 1) "," (coords 0))))

;; -- presentation --

(defn -get-coords
  "From MongoMap, extract coords."
  [{coords :coordinates}]
  (if (empty? coords)
    nil
    {:lat (coords 1) :lon (coords 0)}))

(defn -get-biz-hour-info
  "From MongoMap's hours info for a single day, extract hours."
  [{d :wday c? :is_closed
    oh :open_hour om :open_minute
    ch :close_hour cm :close_minute}]
  (merge {:wday d}
    (if c?
      {:is_closed true}
      {:hours {:open  {:hour oh :minute om}
               :close {:hour ch :minute cm}}})))

(defn -get-address
  ""
  [{a1 :address_1 a2 :address_2 city :city state :state zip :zip}]
  :address {:street (str/join ", " (remove str/blank? [a1 a2]))
            :city city
            :state state
            :zip zip})

;; -- search document --

;; PUBLIC -- because used by business-menu-item/-mk-es-maps.
(defn mk-es-map
  "Given a business mongo-map, create an ElasticSearch map."
  [{:keys [name permalink business_items] :as mg-map}]
  {;; search
   :name name
   :phone_number (-get-phone-number mg-map)
   ;; filter
   :latitude_longitude (-get-lat-lon-str mg-map)
   ;; sort
   :value_score_int (-get-value-score business_items)
   ;; presentation
   :address (-get-address mg-map)
   :coordinates (-get-coords mg-map)
   :hours (map -get-biz-hour-info (:business_hours mg-map))
   :permalink permalink
   })
   
(defn -add-to-idx
  "Given a business mongo-map, convert to es-map and add to index."
  [mg-map]
  (let [es-map (mk-es-map mg-map)]
    ;;
    ;; TODO
    ;; es-doc/put returns a Clojure map.
    ;; To check if successful, use response/ok? or response/conflict?
    ;; 
    ;; With es-doc/put (vs. es-doc/create), you supply the _id separately.
    ;;
    (es-doc/put idx-name mapping-name
                (str (:_id mg-map))
                es-map)))

(defn mk-idx
  "Fetch Businesses from MongoDB and add them to index.  Return count."
  []
  (util/recreate-idx idx-name mapping-types)
  (doseq-cnt -add-to-idx 5000
             (mg/fetch :businesses :where {:active_ind true})))


;; -- Just for testing --
(defn -main [& args]
  (searchzy.util/mongo-connect! cfg/mongo-db-cfg)
  (doseq [doc (take 200 (mg/fetch :businesses :where {:active_ind true}))]
    (println doc)
    (println)))
