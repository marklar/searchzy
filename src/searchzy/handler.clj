(ns searchzy.handler
  (:use compojure.core)
  (:require ;; [somnium.congomongo :as mg]
            [clojure.data.json :as json]
            [compojure.handler :as handler]
            [compojure.route :as route]))

;; COMPOJURE ROUTES
(defroutes app-routes

  (GET "/" [] "Hello, World!")

  (GET "/businesses" [query lat lng & others]
       {:status 200
        :headers {"Content-Type" "application/json; charset=utf-8"}
        ;; :body (mg/fetch-one :businesses :as :json)
        :body (json/write-str {:query query
                               :lat lat, :lng lng
                               :others others})
        })

  (route/resources "/")
  (route/not-found "Not Found"))


;; COMPOJURE APP
(def app
  (handler/site app-routes))
