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
             [inputs :as inputs]
             [business :as biz]
             [responses :as responses]
             [suggestions :as sugg]
             [business-menu-items :as items]
             [bmi :as bmi]]
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

  ;;-- DOCS --

  (GET "/docs" []
       (docs.core/show))

  (GET "/docs/suggestions" []
       (docs.sugg/show))

  (GET "/docs/businesses" []
       (docs.biz/show))

  (GET "/docs/business_menu_items" []
       (docs.items/show))

  ;;-- BUSINESSES --

  (GET (v-path 1 "/businesses")
       [api_key query address lat lon miles hours utc_offset sort from size]
       (if (not (valid-key? api_key))
         ;; not authorized
         (bounce)
         ;; authorized
         (biz/validate-and-search
          {:query query
           :geo-map {:address address, :lat lat, :lon lon, :miles miles}
           :hours hours
           :utc-offset utc_offset
           :sort sort
           :page-map {:from from, :size size}})))

  ;;-- BUSINESS_MENU_ITEMS --

  ;; These results contain aggregate meta-info.
  (GET (v-path 1 "/business_menu_items")
       [api_key item_id address lat lon miles max_miles min_results
        hours utc_offset sort from size]
       (if (not (valid-key? api_key))
         ;;-- not authorized
         (bounce)

         ;;-- authorized

         ;; We have two different search fns to choose from.
         ;; If min_results is provided, we use the newer 'bmi' version.
         ;; If not, we use the (original) 'business_menu_items' one.
         ;;
         ;; TODO: Decide which implementation we want.
         ;;
         (let [search-fn (if (not (clojure.string/blank? min_results))
                           bmi/validate-and-search
                           items/validate-and-search)]
           (search-fn
            {:item-id item_id
             :geo-map {:address address, :lat lat, :lon lon, :miles miles}
             :collar-map {:max-miles max_miles, :min-results min_results}
             :hours hours
             :utc-offset utc_offset
             :sort sort
             :page-map {:from from, :size size}}))))

  ;;-- SUGGESTIONS --

  (GET (v-path 1 "/suggestions")
       [query address lat lon miles size html]
       (let [path (v-path 1 "/suggestions")
             input-map (sugg/mk-input-map path query address lat lon miles size html)
             res (sugg/validate-and-search-v1 input-map)]
         (responses/json-p-ify res)))

  ;; Differences from v1:
  ;; + Adds parameter use_jsonp (default: false).
  ;; + Adds fdb_id to each entity in each section of results.
  (GET (v-path 2 "/suggestions")
       [query address lat lon miles size html use_jsonp]
       (let [path (v-path 2 "/suggestions")
             input-map (sugg/mk-input-map path query address lat lon miles size html)
             res (sugg/validate-and-search-v2 input-map)]
         (if (inputs/true-str? use_jsonp)
           (responses/json-p-ify res)
           res)))

  (route/resources "/")
  (route/not-found "Not Found"))




(util/es-connect! (:elastic-search (cfg/get-cfg)))

;; COMPOJURE APP -- fn :: request-map -> response-map
(def app
  (handler/site app-routes))
