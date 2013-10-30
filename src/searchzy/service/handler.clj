(ns searchzy.service.handler
  (:use compojure.core)
  (:require [searchzy 
             [util :as util]
             [cfg :as cfg]]
            [searchzy.service
             [util :as s-util]]
            [searchzy.service.business
             [core :as biz]]
            [compojure
             [handler :as handler]
             [route :as route]]))


;; COMPOJURE ROUTES
(defroutes app-routes

  (GET "/" [query lat lng & others]
       (s-util/ok-json-response {:query query
                                 :lat lat, :lng lng
                                 :others others}))

  (GET "/business" [query address miles lat lng sort from size & args]
       (biz/validate-and-search query address miles lat lng sort from size))

  (route/resources "/")
  (route/not-found "Not Found"))


(util/es-connect! cfg/elastic-search-cfg)

;; COMPOJURE APP
(def app
  (handler/site app-routes))
