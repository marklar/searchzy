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

(defn -mk-biz-hit-response
  "From ES hit, make service hit."
  [{id :_id
    {n :name a :search_address
     p :permalink l :latitude_longitude} :_source}]
  {:_id id
   :name n
   :address a
   :permalink p
   :lat_lng l
   })

(defn -mk-biz-response
  "From ES response, create service response."
  [{hits-map :hits}]
  (let [n    (:total hits-map)
        hits (:hits hits-map)]
    {:total_hits n :hits (map -mk-biz-hit-response hits)}))

;; COMPOJURE ROUTES
(defroutes app-routes

  (GET "/" [query lat lng & others]
       (s-util/ok-json-response {:query query
                                 :lat lat, :lng lng
                                 :others others}))

  (GET "/business" [query address miles lat lng by-value from size & args]
       (let [;; transform params
             miles      (s-util/str-to-val miles 4.0)
             lat        (s-util/str-to-val lat nil)
             lng        (s-util/str-to-val lng nil)
             by-value?  (s-util/true-str? by-value)
             from       (s-util/str-to-val from 0)
             size       (s-util/str-to-val size 10)
             ;; fetch results
             res  (biz/search query address miles lat lng
                              by-value? from size)]
         (s-util/ok-json-response (-mk-biz-response res))))
          

  (route/resources "/")
  (route/not-found "Not Found"))


;; 1. execute query against index
;; 2. rank results by value score, match score, etc.
;; 3. return:
;;    * the coordinate at the center of the search
;;    * a map: (biz-id, biz-name, biz-address,
;;              biz-permalink, biz-coords,
;;              value-score, match-score)


(util/es-connect! cfg/elastic-search-cfg)

;; COMPOJURE APP
(def app
  (handler/site app-routes))
