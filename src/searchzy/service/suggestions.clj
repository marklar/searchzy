(ns searchzy.service.suggestions
  "For suggestions searches."
  (:use [hiccup.core])
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [util :as util]
             [inputs :as inputs]
             [responses :as responses]
             [business :as biz]]
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
          [:a {:href "#"} name]])])
    
    (if (not (empty? item-hits))
      [:h4.items "Services"])
    (if (not (empty? item-hits))
      [:ul.items
       (for [{id :_id {name :name} :_source} item-hits]
         [:li.ac_item {:id id}
          [:a {:href "#"} name]])])
    
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
  [{id :_id {n :name, a :address, fi :fdb_id, bcis :business_category_ids} :_source}]
  {:id id, :fdb_id fi, :name n, :address a, :business_category_ids bcis})

(defn- mk-simple-hit-response
  "For ES hit of either biz-category or item, make a service hit."
  [{i :_id, {n :name, fdb_id :fdb_id} :_source}]
  {:id i, :fdb_id fdb_id, :name n})

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
  [biz-res cat-res item-res endpoint query biz-cat-ids geo-map page-map html?]
  (let [tmp {:arguments {:query query
                         :business_category_ids biz-cat-ids
                         :geo_filter geo-map
                         :paging page-map
                         :html html?}
             :endpoint endpoint
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
     tmp)))

(defn- mk-biz-cat-id-filter
  [domain biz-cat-ids]
  (if (empty? biz-cat-ids)
    nil
    (let [field-name (if (= domain :business_categories)
                       :_id
                       :business_category_ids)]
      {:term {field-name biz-cat-ids}})))

(defn- mk-filtered-query
  [domain query-str biz-cat-ids]
  {:filtered {:query (util/mk-suggestion-query query-str)
              :filter (mk-biz-cat-id-filter domain biz-cat-ids)}})

(defn- get-results
  "Perform prefix search against names."
  [domain query-str biz-cat-ids {:keys [from size]}]
  (let [es-names (get cfg/elastic-search-names domain)]
    (:hits (es-doc/search (:index es-names)
                          (:mapping es-names)
                          :query  (mk-filtered-query domain query-str biz-cat-ids)
                          :from   from
                          :size   size))))

;;
;; TODO: return biz-categories (order: display_order ASC).
;; 

;; fetch results
;; TODO: in *parallel*.  How?
;;  - pmap: Probably not worth the coordination overhead.
;;  - clojure.core.reducers/map
;;  - agents (uncoordinated, asynchronous)
(defn- search
  [valid-args]
  (let [{:keys [endpoint query business-category-ids
                geo-map page-map html]} valid-args]
    (let [no-q (clojure.string/blank? query)
          biz-results  (if no-q
                         {:total 0, :hits []}
                         (biz/es-search query :prefix
                                        business-category-ids
                                        geo-map nil ; -sort-
                                        page-map))
          item-results (if no-q
                         {:total 0, :hits []}
                         (get-results :items
                                      query business-category-ids page-map))
          cat-results  (get-results :business_categories
                                    query business-category-ids page-map)]
      (responses/ok-json
       (mk-response biz-results cat-results item-results
                    endpoint query business-category-ids
                    geo-map page-map html)))))


;;-- public --

(defn mk-input-map
  [endpoint query business-category-ids address lat lon miles size html]
  {:endpoint endpoint
   :query query
   :business-category-ids business-category-ids
   ;; :geo-map {:address address, :coords {:lat lat, :lon lon}, :miles miles}
   :geo-map {:address address, :lat lat, :lon lon, :miles miles}
   :page-map {:from "0", :size size}
   :html html})

;; v1
(defn validate-and-search-v1
  "Perform 3 searches (in parallel!):
      - businesses (w/ filtering)
      - business_categories
      - items"
  [input-args]
  (util/validate-and-search input-args inputs/suggestion-clean-input-v1 search))

;; v2
(defn validate-and-search-v2
  "Perform 3 searches (in parallel!):
      - businesses (w/ filtering)
      - business_categories
      - items"
  [input-args]
  (util/validate-and-search input-args inputs/suggestion-clean-input-v2 search))
