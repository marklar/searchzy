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


;; (defn- mk-input-maps
;;   [address lat lon miles wday hour minute from size]
;;   [geo-map   {:address address, :lat lat, :lon lon, :miles miles}
;;    hours-map {:wday wday, :hour hour, :minute minute}
;;    page-map  {:from from, :size size}])


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
       ;; hours = 3-15:30
       [api_key query address lat lon miles hours wday hour minute sort from size]
       (if (not (valid-key? api_key))
         ;; not authorized
         (bounce)
         ;; authorized
         (let [geo-map   {:address address, :lat lat, :lon lon, :miles miles}
               hours-map {:hours hours, :wday wday, :hour hour, :minute minute}
               page-map  {:from from, :size size}]
           (biz/validate-and-search query geo-map hours-map sort page-map))))

  ;; These results contain aggregate meta-info.
  (GET (v-path 1 "/business_menu_items")
       [api_key item_id address lat lon miles hours wday hour minute sort from size]
       (if (not (valid-key? api_key))
         ;; not authorized
         (bounce)
         ;; authorized
         (let [geo-map   {:address address, :lat lat, :lon lon, :miles miles}
               hours-map {:hours hours, :wday wday, :hour hour, :minute minute}
               page-map  {:from from, :size size}]
           (items/validate-and-search item_id geo-map hours-map sort page-map))))

  (GET (v-path 1 "/suggestions")
       [query address lat lon miles size html]

       (let [geo-map  {:address address, :lat lat, :lon lon, :miles miles}
             page-map {:from "0", :size size}]
         (responses/json-p-ify
          (sugg/validate-and-search query geo-map page-map html))))

  (route/resources "/")
  (route/not-found "Not Found"))


(util/es-connect! (:elastic-search (cfg/get-cfg)))

;; COMPOJURE APP -- fn :: request-map -> response-map
(def app
  (handler/site app-routes))
