(ns searchzy.handler
  (:use compojure.core)
  (:require [searchzy 
             [util :as util]
             [cfg :as cfg]]
            [searchzy.search
             [util :as s-util]
             [business :as biz]]
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
       (biz/biz-search query address miles lat lng sort from size))

  (route/resources "/")
  (route/not-found "Not Found"))


(util/es-connect! cfg/elastic-search-cfg)

;; COMPOJURE APP
(def app
  (handler/site app-routes))
