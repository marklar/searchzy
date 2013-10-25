(ns searchzy.handler
  (:use compojure.core)
  (:import [java.lang.Double])
  (:require [somnium.congomongo :as mg]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [compojure.handler :as handler]
            [compojure.route :as route]
            ;; [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.native :as es]
            [clojurewerkz.elastisch.native.index :as es-idx]
            [clojurewerkz.elastisch.native.document :as es-doc]))

;; MONGO CONNECTION (global)
(def conn
  (mg/make-connection "centzy2_development"
                      :host "127.0.0.1"
                      :port 27017))
(mg/set-connection! conn)


;; ELASTIC SEARCH
;; -- REST
;; (esr/connect! "http://127.0.0.1:9200")

;; -- NATIVE
(def cluster-name "elasticsearch_markwong-vanharen")
(es/connect! [["127.0.0.1", 9300]]
             {"cluster.name" cluster-name})


(def mapping-name "business")
(def idx-name "businesses")
(def mapping-types
  ;; Store a field only if you need it returned to you in the search results.
  ;; The entire JSON of the document is stored anyway, so you can ask for that.
  {mapping-name
   {:properties
    {:id                       {:type "string" :index "not_analyzed" :include_in_all false}
     :name                     {:type "string" }
     :permalink                {:type "string" :index "not_analyzed" :include_in_all false}
     :latitude_longitude       {:type "geo_point" :null_value "66.666,66.666" :include_in_all false}
     :search_address           {:type "string" }
     :business_category_names  {:type "string" :analyzer "keyword"}
     :item_category_names      {:type "string" :analyzer "keyword"}
     :value_score_int          {:type "integer" :null_value 0 :include_in_all false}
     }}})

(defn recreate-idx []
  (es-idx/delete idx-name)
  (if (not (es-idx/exists? idx-name))
    (es-idx/create idx-name :mappings mapping-types)))

(defn get-biz-cat-names
  "From business_category_ids, get their names by finding in MongoDB.  Cache?"
  [biz-cat-ids]
  (if (empty? biz-cat-ids)
    []
    (let [cats (mg/fetch :business_categories
                         :where {:_id {:$in biz-cat-ids}}
                         :only [:name])]
      (distinct (map :name cats)))))

(defn get-value-score
  "For seq of business_item maps, find highest value_score (as % int)."
  [biz-items]
  (let [scores (filter number? (map :value_score biz-items))]
    (int (* 100 (apply max (cons 0 scores))))))

(defn get-lat-lng-str
  [coords]
  (if (empty? coords)
    nil
    ;; (let [[lat lng] coords] (str lat "," lng))))
    (str (coords 0) "," (coords 1))))

(defn mk-es-map
  "Given a business map, create an ElasticSearch map."
  [{id :_id nm :name pl :permalink
    coords :coordinates
    a1 :address_1 a2 :address_2
    bc-ids :business_category_ids
    items :business_items}]
  (let [lat-lng-str (if (empty? coords)
                      nil
                      (let [[lat lng] coords] (str lat "," lng)))]
    {:id id
     :name nm
     :permalink pl
     :latitude_longitude lat-lng-str
     :search_address (str/join " " (filter str/blank? [a1 a2]))
     :business_category_names (get-biz-cat-names bc-ids)
     :item_category_names (distinct (map :item_name items))
     :value_score_int (get-value-score items)
     }))
   
(defn add-to-idx
  "Given a business mongo-map, convert to es-map and add to index."
  [mg-map]
  (let [es-map (mk-es-map mg-map)]
    ;; (println mg-map)
    ;; (println es-map)
    ;; (println)
    (es-doc/create idx-name mapping-name es-map)))

(def *cnt* 0)

;; COMPOJURE ROUTES
(defroutes app-routes

  (GET "/" [] "Hello, World!")

  (GET "/businesses" [query lat lng & others]
       (let [biz (mg/fetch-one :businesses :as :json)]
         {:status 200
          :headers {"Content-Type" "application/json; charset=utf-8"}
          :body biz
          ;; :body (json/write-str {:query query
          ;;                        :lat lat, :lng lng
          ;;                        :others others})
          }))

  ;; Should be a POST!
  (GET "/index_businesses" []
       (let [cnt (atom 0)]
         (recreate-idx)
         (doseq [biz (mg/fetch :businesses :where {:active_ind true})]
           (add-to-idx biz)
           (swap! cnt inc)
           (if (= 0 (mod @cnt 1000))
             (println @cnt))
           (str "<h1>Indexing Businesses</h1><p>number of records: " @cnt "</p>"))))

  (route/resources "/")
  (route/not-found "Not Found"))


;; COMPOJURE APP
(def app
  (handler/site app-routes))
