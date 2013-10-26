(ns searchzy.handler
  (:use compojure.core)
  (:require [clojure.data.json :as json]
            [compojure.handler :as handler]
            [compojure.route :as route]))

;; COMPOJURE ROUTES
(defroutes app-routes

  (GET "/" [query lat lng & others]
       {:status 200
        :headers {"Content-Type" "application/json; charset=utf-8"}
        :body (json/write-str {:query query
                               :lat lat, :lng lng
                               :others others})
        })

  (route/resources "/")
  (route/not-found "Not Found"))


;; COMPOJURE APP
(def app
  (handler/site app-routes))
