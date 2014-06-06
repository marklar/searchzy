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
             [lists :as docs.lists]
             [business-menu-items :as docs.items]]
            [searchzy.service
             [authorization :as auth]
             [catch-exceptions :as catch]
             [biz-counts :as biz-counts]
             [inputs :as inputs]
             [mean-prices :as mean-prices]
             [business :as biz]
             [responses :as responses]]
            [searchzy.service.suggestions
             [core :as sugg]]
            [searchzy.service.bmis
             [core :as items]]
            [searchzy.service.lists :as lists]
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

  (GET "/" [& args]
       (responses/ok-json
        {:message "Welcome to Searchzy!"
         :params args}))

  ;;-- DOCUMENTATION --

  (GET "/docs" []
       (docs.core/show))

  (GET "/docs/suggestions" []
       (docs.sugg/show))

  (GET "/docs/businesses" []
       (docs.biz/show))

  (GET "/docs/business_menu_items" []
       (docs.items/show))

  (GET "/docs/lists" []
       (docs.lists/show))

  ;;-- MEAN_PRICES --

  ;; TODO: permalink
  (GET (v-path 1 "/mean_prices")
       [permalink item_category_ids miles
        :as {{:strs [polygon]} :query-params}]
       (mean-prices/validate-and-search
        {:permalink permalink
         :item-category-ids item_category_ids
         :miles miles}))

  ;;-- BUSINESSES --

  ;; TODO:  Make geo-map optional.  If missing, use coords from item.
  (GET (v-path 1 "/business_counts")
       [item_id address lat lon miles
        :as {{:strs [polygon]} :query-params}]
       (biz-counts/validate-and-search
        {:item-id item_id
         :geo-map {:polygon polygon, :address address, :lat lat, :lon lon, :miles miles}}))

  (GET (v-path 1 "/lists")
       [location_id seo_business_category_id seo_region_id seo_item_id
        area_type state
        address lat lon miles
        from size
        :as {{:strs [polygon]} :query-params}]
       (lists/validate-and-search
        {:location-id location_id
         :seo-business-category-id seo_business_category_id
         :seo-region-id seo_region_id
         :seo-item-id seo_item_id
         :area-type area_type
         :state state
         :geo-map {:polygon polygon, :address address, :lat lat, :lon lon, :miles miles}
         :page-map {:from from, :size size}
         }))

  (GET (v-path 1 "/businesses")
       [query business_category_ids
        address lat lon miles
        hours utc_offset sort from size
        :as {{:strs [polygon]} :query-params}]
       (biz/validate-and-search
        {:query query
         :business-category-ids business_category_ids
         :geo-map {:polygon polygon, :address address, :lat lat, :lon lon, :miles miles}
         :hours hours
         :utc-offset utc_offset
         :sort sort
         :page-map {:from from, :size size}}))

  ;;-- BUSINESS_MENU_ITEMS --

  ;; >>> v1 <<<
  (GET (v-path 1 "/business_menu_items")
       [item_id address lat lon miles
        hours utc_offset sort from size include_unpriced
        :as {{:strs [polygon]} :query-params}]
       (items/validate-and-search
        "v1"
        {:item-id item_id
         :include-unpriced include_unpriced
         :geo-map {:polygon polygon, :address address, :lat lat, :lon lon, :miles miles}
         :hours hours
         :utc-offset utc_offset
         :sort sort
         :page-map {:from from, :size size}}))

  ;; >>> v2 <<<
  (GET (v-path 2 "/business_menu_items")
       [item_id address lat lon miles
        hours utc_offset sort from size include_unpriced
        :as {{:strs [polygon]} :query-params}]
       (items/validate-and-search
        "v2"
        {:item-id item_id
         :include-unpriced include_unpriced
         :geo-map {:polygon polygon, :address address, :lat lat, :lon lon, :miles miles}
         :hours hours
         :utc-offset utc_offset
         :sort sort
         :page-map {:from from, :size size}}))

  ;;-- SUGGESTIONS --

  (GET (v-path 1 "/suggestions")
       [query business_category_ids
        address lat lon miles
        size html
        :as {uri :uri, {:strs [polygon]} :query-params}]
       (let [input-map (sugg/mk-input-map uri query business_category_ids
                                          address lat lon miles polygon
                                          size html nil)
             res (sugg/validate-and-search-v1 input-map)]
         (responses/json-p-ify res)))

  ;; Differences from v1:
  ;; + Adds parameter use_jsonp (default: false).
  ;; + Adds fdb_id to each entity in each section of results.
  ;; + Allows utc_offset string to include appropriate hours_today info in response.
  (GET (v-path 2 "/suggestions")
       [query business_category_ids
        address lat lon miles utc_offset
        size
        html use_jsonp
        :as {uri :uri, {:strs [polygon]} :query-params}]
       (let [input-map (sugg/mk-input-map uri query business_category_ids
                                          address lat lon miles polygon
                                          size html utc_offset)
             res (sugg/validate-and-search-v2 input-map)]
         ;; possibly wrap in jsonp
         (if (inputs/true-str? use_jsonp)
           (responses/json-p-ify res)
           res)))

  (route/resources "/")
  (route/not-found "Not Found"))


;;---------------------------------

(util/es-connect! (:elastic-search (cfg/get-cfg)))

;; COMPOJURE APP -- fn :: request-map -> response-map
(def app
  (handler/site (-> app-routes
                    catch/catch-exceptions
                    auth/authorize)))

