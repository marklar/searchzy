(ns searchzy.handler
  (:use compojure.core)
  (:require [searchzy.search.util :as util]
            [compojure.handler :as handler]
            [compojure.route :as route]))

;; COMPOJURE ROUTES
(defroutes app-routes

  (GET "/" [query lat lng & others]
       (util/ok-json-response {:query query
                               :lat lat, :lng lng
                               :others others}))

  (GET "/business" [query lat lng & others]
       (util/ok-json-response {:a 1}))

  (route/resources "/")
  (route/not-found "Not Found"))


;; 1. execute query against index
;; 2. rank results by value score, match score, etc.
;; 3. return:
;;    * the coordinate at the center of the search
;;    * a map: (biz-id, biz-name, biz-address,
;;              biz-permalink, biz-coords,
;;              value-score, match-score)

;; COMPOJURE APP
(def app
  (handler/site app-routes))
