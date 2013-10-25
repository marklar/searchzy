(ns searchzy.handler
  (:use compojure.core)
  (:require [searchzy.business :as business]
            [searchzy.item-category :as item-category]
            [searchzy.business-category :as business-category]
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


(defn build-index
  "Perform 'f'.  Return HTML string including 'count' returned from f."
  [f name]
  (let [cnt (f)]
    (str "<h1>Indexing " name "</h1>"
         "<p>number of records: " cnt "</p>")))

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
  (GET "/index/businesses" []
       (build-index business/mk-idx "Businesses"))

  ;; Should be a POST!
  (GET "/index/item_categories" []
       (build-index item-category/mk-idx "ItemCategories"))

  ;; Should be a POST!
  (GET "/index/business_categories" []
       (build-index business-category/mk-idx "BusinessCategories"))

  (route/resources "/")
  (route/not-found "Not Found"))


;; COMPOJURE APP
(def app
  (handler/site app-routes))
