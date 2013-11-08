(ns searchzy.service.handler
  (:use compojure.core)
  (:require [searchzy 
             [util :as util]
             [cfg :as cfg]]
            [searchzy.service
             [docs :as docs]
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
       (docs/show))

  (GET "/docs/suggestions" []
       (docs/suggestions))

  (GET (v-path 1 "/businesses")
       [query address lat lon sort from size]
       (biz/validate-and-search query address lat lon sort from size))

  (GET (v-path 1 "/business_menu_items")
       [item_id address lat lon from size]
       (items/validate-and-search item_id address lat lon from size))

  (GET (v-path 1 "/suggestions")
       [query address lat lon miles size html]
       (sugg/validate-and-search query address lat lon miles size html))

  (route/resources "/")
  (route/not-found "Not Found"))


(util/es-connect! cfg/elastic-search-cfg)

;; COMPOJURE APP
(def app
  (handler/site app-routes))
