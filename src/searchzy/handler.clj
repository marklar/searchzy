(ns searchzy.handler
  (:use compojure.core)
  (:require [clojure.data.json :as json]
            [compojure.handler :as handler]
            [compojure.route :as route]))

(def json-headers
  {"Content-Type" "application/json; charset=utf-8"})

(defn ok-json-response
  [obj]
  {:status 200
   :headers json-headers
   :body (json/write-str obj)})
  

;; COMPOJURE ROUTES
(defroutes app-routes

  (GET "/" [query lat lng & others]
       (ok-json-response {:query query
                          :lat lat, :lng lng
                          :others others}))

  (route/resources "/")
  (route/not-found "Not Found"))


;; COMPOJURE APP
(def app
  (handler/site app-routes))
