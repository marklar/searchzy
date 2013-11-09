(ns searchzy.service.handler
  (:use compojure.core)
  (:require [searchzy 
             [util :as util]
             [cfg :as cfg]]
            [searchzy.service.docs
             [core :as core-docs]
             [suggestions :as sugg-docs]
             [businesses :as biz-docs]
             [business-menu-items :as item-docs]]
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

  (GET "/" []
       "Welcome to Searchzy!")

  (GET "/docs" []
       (core-docs/show))

  (GET "/docs/suggestions" []
       (sugg-docs/show))

  (GET "/docs/businesses" []
       (biz-docs/show))

  (GET "/docs/business_menu_items" []
       (item-docs/show))

  (GET (v-path 1 "/businesses")
       [query address lat lon miles sort from size]
       (let [geo-map  {:address address, :lat lat, :lon lon, :miles miles}
             page-map {:from from, :size size}]
         (biz/validate-and-search query geo-map sort page-map)))

  (GET (v-path 1 "/business_menu_items")
       [item_id address lat lon miles from size]
       (let [geo-map  {:address address, :lat lat, :lon lon, :miles miles}
             page-map {:from from, :size size}]
         (items/validate-and-search item_id geo-map page-map)))

  (GET (v-path 1 "/suggestions")
       [query address lat lon miles size html]
       (let [geo-map  {:address address, :lat lat, :lon lon, :miles miles}
             page-map {:from "0", :size size}]
         (responses/json-p-ify
          (sugg/validate-and-search query geo-map page-map html))))

  (route/resources "/")
  (route/not-found "Not Found"))


(util/es-connect! (:elastic-search (cfg/get-cfg)))

;; COMPOJURE APP
(def app
  (handler/site app-routes))
