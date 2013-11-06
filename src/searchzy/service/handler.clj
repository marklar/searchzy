(ns searchzy.service.handler
  (:use compojure.core)
  (:require [searchzy 
             [util :as util]
             [cfg :as cfg]]
            [searchzy.service
             [responses :as responses]
             [suggestions :as sugg]
             [business-menu-items :as items]]
            [searchzy.service.business
             [core :as biz]]
            [compojure
             [handler :as handler]
             [route :as route]]))

(def current-version "v1")
(defn v-path
  "Create path by appending version number."
  [version-number path]
  (str "/v" version-number path))

;; COMPOJURE ROUTES
(defroutes app-routes

  (GET "/" [query lat lon & others]
       (responses/ok-json {:query query
                           :lat lat, :lon lon
                           :others others}))

  (GET (v-path 1 "/businesses.json")
       [query address lat lon sort from size]
       (biz/validate-and-search query address lat lon sort from size))

  (GET (v-path 1 "/business_menu_items.json")
       [item_id address lat lon from size]
       (items/validate-and-search item_id address lat lon from size))

  (GET (v-path 1 "/suggestions.json")
       [query address lat lon]
       (sugg/validate-and-search query address lat lon))

  (route/resources "/")
  (route/not-found "Not Found"))


(util/es-connect! cfg/elastic-search-cfg)

;; COMPOJURE APP
(def app
  (handler/site app-routes))
