(ns searchzy.handler
  (:use compojure.core)
  (:require [somnium.congomongo :as mg]
            [clojure.data.json :as json]
            [compojure.handler :as handler]
            [compojure.route :as route]
            ;; [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.native :as es]
            [clojurewerkz.elastisch.native.index :as es-idx]
            )
  )


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


(def mapping-types
  ;; Store a field only if you need it returned to you in the search results.
  ;; The entire JSON of the document is stored anyway.  Can ask for that.
  {"businesses"
   {:properties
    {:_id                      {:type "string" :index "not_analyzed" :include_in_all false}
     :name                     {:type "string" }
     :permalink                {:type "string" :index "not_analyzed" :include_in_all false}
     :latitude_longitude       {:type "geo_point" :null_value "66.666,66.666" :include_in_all false}
     :search_address           {:type "string" }
     :business_category_names  {:type "string" :analyzer "keyword"}
     :item_category_names      {:type "string" :analyzer "keyword"}
     :value_score_int          {:type "integer" :null_value 0 :include_in_all false}
     }}})

(def idx-name "businesses")
(es-idx/delete idx-name)
(if (not (es-idx/exists? idx-name))
  (es-idx/create idx-name :mappings mapping-types))



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
       "<h1>Indexing Businesses</h1><p>Please wait a bit.</p>")

  (route/resources "/")
  (route/not-found "Not Found"))


;; COMPOJURE APP
(def app
  (handler/site app-routes))
