(ns searchzy.service.suggestions
  (:use [hiccup.core])
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [util :as util]
             [inputs :as inputs]
             [validate :as validate]
             [geo :as geo]
             [responses :as responses]
             [query :as q]]
            [searchzy.service.business
             [search :as biz-search]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

;;
;; The HTML we want to reproduce:
;;   - _logo_search_banner.html.haml
;;   - app/assets/javascripts/landings/search.js.coffee
;;

(defn- mk-addr-str
  [{:keys [street city state zip]}]
  (str (clojure.string/join ", " [street city state]) " " zip))

(defn- mk-html
  [biz-hits cat-hits item-hits]
  (html
   [:div#auto_complete_list

    (if (not (empty? cat-hits))
      [:h4.business_categories "Categories"])
    (if (not (empty? cat-hits))
      [:ul.buinesss_categories
       (for [{id :_id {name :name} :_source} cat-hits]
         [:li.ac_biz_category {:id id}
          [:a {:href "#", :data-biz-category name} name]])])
    
    ;; FIXME: This really should be .items, not .item_categories
    (if (not (empty? item-hits))
      [:h4.items "Services"])
    (if (not (empty? item-hits))
      [:ul.items
       (for [{id :_id {name :name} :_source} item-hits]
         [:li.ac_item {:id id}
          [:a {:href "#", :data-item name} name]])])
    
    (if (not (empty? biz-hits))
      [:h4.businesses "Businesses"])
    (if (not (empty? biz-hits))
      [:ul.businesses
       (for [{id :_id {:keys [address permalink name]} :_source} biz-hits]
         [:li.ac_biz_item {:id (str "ac_biz_" id)}
          [:a.auto_complete_list_business {:href (str "/place/" permalink)}
           (str name "&nbsp;")
           [:span.address (mk-addr-str address)]]])])
    
    [:ul
     [:li
      [:a#dropdown_submit_search {:href "#"} "Search all businesses"]]]]))

(defn- mk-biz-hit-response
  "From ES biz hit, make service hit."
  [{id :_id {n :name a :address} :_source}]
  {:id id, :name n, :address a})

(defn- mk-simple-hit-response
  "For ES hit of either biz-category or item, make a service hit."
  [{i :_id, {n :name} :_source}]
  {:id i, :name n})

(defn- mk-res-map
  [f hits-map]
  {:count (:total hits-map)
   :hits  (map f (:hits hits-map))})

(defn- mk-arguments
  [query geo-map page-map html?]
  {:query query
   :geo_filter geo-map
   :paging page-map
   :html html?})

(defn- mk-response
  "From ES response, create service response."
  [biz-res cat-res item-res query geo-map page-map html?]
  (let [partial {:arguments {:query query
                             :geo_filter geo-map
                             :paging page-map
                             :html html?}
                 :endpoint "/v1/suggestions"   ; TODO: pass this in
                 }]
    (merge
     (if html?
       {:html (apply mk-html (map :hits [biz-res cat-res item-res]))}
       {:results {:businesses
                  (mk-res-map mk-biz-hit-response    biz-res)
                  :business_categories
                  (mk-res-map mk-simple-hit-response cat-res)
                  :items
                  (mk-res-map mk-simple-hit-response item-res)}})
     partial)))

(defn- get-results
  "Perform prefix search against names."
  [domain query {:keys [from size]}]
  (let [es-names (get cfg/elastic-search-names domain)]
    (:hits (es-doc/search (:index es-names) (:mapping es-names)
                          :query  (util/mk-suggestion-query query)
                          :from   from
                          :size   size))))

;; fetch results
;; TODO: in *parallel*.  How?
;;  - pmap: Probably not worth the coordination overhead.
;;  - clojure.core.reducers/map
;;  - agents (uncoordinated, asynchronous)

(defn validate-and-search
  "Perform 3 searches (in parallel!):
      - businesses (w/ filtering)
      - business_categories
      - items"
  [input-query input-geo-map input-page-map input-html]

  ;; Validate query.
  (let [query (q/normalize input-query)]
    (if (clojure.string/blank? query)
      (validate/response-bad-query input-query query)
      
      ;; Validate location info.
      (let [geo-map (inputs/mk-geo-map input-geo-map)]
        (if (nil? geo-map)
          (validate/response-bad-location input-geo-map)
          
          ;; OK, do searches.
          (let [page-map (inputs/mk-page-map input-page-map)
                html? (inputs/true-str? input-html)
                biz-results  (biz-search/get-results query :prefix
                                                     geo-map
                                                     nil  ; -sort-
                                                     page-map)
                cat-results  (get-results :business_categories query page-map)
                item-results (get-results :items query page-map)]

            (responses/ok-json
             (mk-response biz-results cat-results item-results
                          query geo-map page-map html?))))))))
