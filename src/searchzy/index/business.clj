(ns searchzy.index.business
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [searchzy.cfg :as cfg]
            [clojure.string :as str]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))

(def idx-name (:businesses cfg/index-names))
(def mapping-name "business")

;; Store a field only if you need it returned to you in the search results.
;; The entire JSON of the document is stored anyway, so you can ask for that.
;;
;; Each index may have one OR MORE mapping-types (each w/ a diff mapping-name),
;; if you put more than one type of document in your index.
;; I don't think we want to do that.
;;
(def mapping-types
  {mapping-name
   {:properties

    ;; TODO: Add hours information.
    {:name                     {:type "string"}

     :permalink                {:type "string"
                                :index "not_analyzed"
                                :include_in_all false}

     :latitude_longitude       {:type "geo_point"
                                :null_value "66.666,66.666"
                                :include_in_all false}

     :search_address           {:type "string"}

     ;; TODO: Can remove these.
     :business_category_names  {:type "string"
                                :analyzer "keyword"}

     :business_category_ids    {:type "string"}

     ;; TODO: Can remove these.
     :item_category_names      {:type "string"
                                :analyzer "keyword"}

     :phone_number             {:type "string"}

     ;; From the embedded BusinessItems, extract prices.

     :value_score_int          {:type "integer"
                                :null_value 0
                                :include_in_all false}
     }}})

;; (defn -get-biz-cat-names
;;   "From business_category_ids, get names by finding in MongoDB.  Cache?"
;;   ;; This is relatively expensive, so if we don't need to search by
;;   ;; biz cat names, we should remove this from the index documents.
;;   [biz-cat-ids]
;;   (if (empty? biz-cat-ids)
;;     []
;;     (let [cats (mg/fetch :business_categories
;;                          :where {:_id {:$in biz-cat-ids}}
;;                          :only [:name])]
;;       (distinct (map :name cats)))))

(defn -get-value-score
  "For seq of business_item maps, find highest value_score (as % int)."
  [biz-items]
  (let [scores (filter number? (map :value_score biz-items))]
    (int (* 100 (apply max (cons 0 scores))))))

(defn -get-lat-lon-str
  "From 2 nums, REVERSE ORDER, create string.  [10.3 40.1] => '10.3,40.1' "
  ;; In MongoDB, the coords are stored backwards (i.e. first lon, then lat).
  [coords]
  (if (empty? coords)
    nil
    (str (coords 1) "," (coords 0))))

(defn -get-country-code
  ""
  [country-name]
  ;; Use map.
  ;; http://en.wikipedia.org/wiki/List_of_country_calling_codes#Alphabetical_listing_by_country_or_region
  (if (= "US" country-name)
    "1"
    nil))

(defn -get-phone-number
  ""
  [country-name area num]
  (str/join "-" (remove str/blank? [(-get-country-code country-name) area num])))


(defn -mk-es-map
  "Given a business mongo-map, create an ElasticSearch map."
  [{_id :_id nm :name pl :permalink
    coords :coordinates a1 :address_1 a2 :address_2
    area :phone_area_code num :phone_number
    country-name :country
    bc-ids :business_category_ids items :business_items}]
  {:name nm :permalink pl
   :latitude_longitude (-get-lat-lon-str coords)
   :search_address (str/join " " (remove str/blank? [a1 a2]))
   ;; :business_category_names (-get-biz-cat-names bc-ids)
   :business_category_ids (map str bc-ids)
   :phone_number (-get-phone-number country-name area num)
   :item_category_names (distinct (map :item_name items))
   :value_score_int (-get-value-score items)
   })
   
(defn -add-to-idx
  "Given a business mongo-map, convert to es-map and add to index."
  [mg-map]
  (let [es-map (-mk-es-map mg-map)]
    ;;
    ;; TODO
    ;; es-doc/put returns a Clojure map.
    ;; To check if successful, use response/ok? or response/conflict?
    ;; 
    ;; With es-doc/put (vs. es-doc/create), you supply the _id separately.
    ;;
    (es-doc/put idx-name
                mapping-name
                (str (:_id mg-map))
                es-map)))

(defn mk-idx
  "Fetch Businesses from MongoDB and add them to index.  Return count."
  []
  (util/recreate-idx idx-name mapping-types)
  (doseq-cnt -add-to-idx 5000
             (mg/fetch :businesses :where {:active_ind true})))
