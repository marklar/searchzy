(ns searchzy.handler
  (:use compojure.core)
  (:require [searchzy.business :as business]
            [searchzy.item-category :as item-category]
            [somnium.congomongo :as mg]
            [clojure.data.json :as json]
            [compojure.handler :as handler]
            [compojure.route :as route]))

;; MONGO CONNECTION (global)
(def conn
  (mg/make-connection "centzy2_development"
                      :host "127.0.0.1"
                      :port 27017))
(mg/set-connection! conn)


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
       (let [cnt (business/mk-idx)]
         (str "<h1>Indexing Businesses</h1>"
              "<p>number of records: " cnt "</p>")))

  ;; Should be a POST!
  (GET "/index_item_categories" []
       (let [cnt (item-category/mk-idx)]
         (str "<h1>Indexing ItemCategories</h1>"
              "<p>number of records: " cnt "</p>")))

  (route/resources "/")
  (route/not-found "Not Found"))


;; COMPOJURE APP
(def app
  (handler/site app-routes))
