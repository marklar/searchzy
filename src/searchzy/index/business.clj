(ns searchzy.index.business
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [clojure.string :as str]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))

(def idx-name "businesses")
(def mapping-name "business")

;; Store a field only if you need it returned to you in the search results.
;; The entire JSON of the document is stored anyway, so you can ask for that.
(def mapping-types
  {mapping-name
   {:properties
    {:name                     {:type "string"}

     :permalink                {:type "string"
                                :index "not_analyzed"
                                :include_in_all false}

     :latitude_longitude       {:type "geo_point"
                                :null_value "66.666,66.666"
                                :include_in_all false}

     :search_address           {:type "string"}

     :business_category_names  {:type "string"
                                :analyzer "keyword"}

     :item_category_names      {:type "string"
                                :analyzer "keyword"}

     :value_score_int          {:type "integer"
                                :null_value 0
                                :include_in_all false}
     }}})

(defn -get-biz-cat-names
  "From business_category_ids, get names by finding in MongoDB.  Cache?"
  [biz-cat-ids]
  (if (empty? biz-cat-ids)
    []
    (let [cats (mg/fetch :business_categories
                         :where {:_id {:$in biz-cat-ids}}
                         :only [:name])]
      (distinct (map :name cats)))))

(defn -get-value-score
  "For seq of business_item maps, find highest value_score (as % int)."
  [biz-items]
  (let [scores (filter number? (map :value_score biz-items))]
    (int (* 100 (apply max (cons 0 scores))))))

(defn -get-lat-lng-str
  "From 2 nums, create string.  [10.3 40.1] => '10.3,40.1' "
  [coords]
  (if (empty? coords)
    nil
    (str (coords 0) "," (coords 1))))

(defn -mk-es-map
  "Given a business mongo-map, create an ElasticSearch map."
  [{_id :_id nm :name pl :permalink
    coords :coordinates a1 :address_1 a2 :address_2
    bc-ids :business_category_ids items :business_items}]
  {:name nm :permalink pl
   :latitude_longitude (-get-lat-lng-str coords)
   :search_address (str/join " " (remove str/blank? [a1 a2]))
   :business_category_names (-get-biz-cat-names bc-ids)
   :item_category_names (distinct (map :item_name items))
   :value_score_int (-get-value-score items)
   })
   
(defn -add-to-idx
  "Given a business mongo-map, convert to es-map and add to index."
  [mg-map]
  (let [es-map (-mk-es-map mg-map)]
    ;; TODO
    ;; es-doc/put returns a Clojure map.
    ;; To check if successful, use response/ok? or response/conflict?
    ;; 
    ;; With es-doc/put (vs. es-doc/create), you supply the _id separately.
    (es-doc/put idx-name mapping-name
                (str (:_id mg-map))
                es-map)))

(defn mk-idx
  "Fetch Businesses from MongoDB and add them to index.  Return count."
  []
  (util/recreate-idx idx-name mapping-types)
  (doseq-cnt -add-to-idx 5000
             (mg/fetch :businesses
                       :where {:active_ind true})))
