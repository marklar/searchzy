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
       [query address lat lon sort from size]
       (biz/validate-and-search query address lat lon sort from size))

  (GET (v-path 1 "/business_menu_items")
       [item_id address lat lon miles from size]
       (items/validate-and-search item_id address lat lon miles from size))

  (GET (v-path 1 "/suggestions")
       [query address lat lon miles size html]
       (responses/json-p-ify
        (sugg/validate-and-search query address lat lon miles size html)))

  (route/resources "/")
  (route/not-found "Not Found"))


(util/es-connect! (:elastic-search (cfg/get-cfg)))

;; COMPOJURE APP
(def app
  (handler/site app-routes))
