(ns searchzy.service.handler
  "Defines Compojure app.  Connects to ElasticSearch."
  (:use compojure.core)
  (:require [searchzy 
             [util :as util]
             [cfg :as cfg]]
            [searchzy.service.docs
             [core :as docs.core]
             [suggestions :as docs.sugg]
             [businesses :as docs.biz]
             [business-menu-items :as docs.items]]
            [searchzy.service
             [business :as biz]
             [responses :as responses]
             [suggestions :as sugg]
             [business-menu-items :as items]]
            [compojure
             [handler :as handler]
             [route :as route]]))

(defn- valid-key?
  [api-key]
  (let [lock (:api-key (cfg/get-cfg))]
    (if (nil? lock)
      true
      (= api-key lock))))

(defn- bounce []
  (responses/json-p-ify
   ;; Using "forbidden" (403) instead of "unauthorized" (401)
   ;; because I don't want to deal with authentication headers.
   (responses/forbidden-json {:error "Not authorized."})))

(def current-version "v1")
(defn v-path
  "Create path by appending version number."
  [version-number path]
  (str "/v" version-number path))

;; COMPOJURE ROUTES
(defroutes app-routes

  (GET "/" [& args]
       (responses/ok-json
        {:message "Welcome to Searchzy!"
         :params args}))

  (GET "/docs" []
       (docs.core/show))

  (GET "/docs/suggestions" []
       (docs.sugg/show))

  (GET "/docs/businesses" []
       (docs.biz/show))

  (GET "/docs/business_menu_items" []
       (docs.items/show))

  (GET (v-path 1 "/businesses")
       [api_key query address lat lon miles hours sort from size]
       (if (not (valid-key? api_key))
         ;; not authorized
         (bounce)
         ;; authorized
         (biz/validate-and-search
          {:query query
           :geo-map {:address address, :lat lat, :lon lon, :miles miles}
           :hours hours
           :sort sort
           :page-map {:from from, :size size}})))

  ;; These results contain aggregate meta-info.
  (GET (v-path 1 "/business_menu_items")
       [api_key item_id address lat lon miles hours sort from size]
       (if (not (valid-key? api_key))
         ;; not authorized
         (bounce)
         ;; authorized
         (items/validate-and-search
          {:item-id item_id
           :geo-map {:address address, :lat lat, :lon lon, :miles miles}
           :hours hours
           :sort sort
           :page-map {:from from, :size size}})))

  (GET (v-path 1 "/suggestions")
       [query address lat lon miles size html]
       (responses/json-p-ify
        (sugg/validate-and-search 
         {:query query
          :geo-map {:address address, :lat lat, :lon lon, :miles miles}
          :page-map {:from "0", :size size}
          :html html})))

  (route/resources "/")
  (route/not-found "Not Found"))


(util/es-connect! (:elastic-search (cfg/get-cfg)))

;; COMPOJURE APP -- fn :: request-map -> response-map
(def app
  (handler/site app-routes))
