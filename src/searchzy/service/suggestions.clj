(ns searchzy.service.suggestions
  (:use [hiccup.core])
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [inputs :as inputs]
             [geo :as geo]
             [responses :as responses]
             [query :as q]]
            [searchzy.service.business
             [validate :as validate]
             [search :as biz-search]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

;;
;; The HTML we want to reproduce:
;;   - _logo_search_banner.html.haml
;;   - app/assets/javascripts/landings/search.js.coffee
;;

(defn -mk-addr-str
  [{:keys [street city state zip]}]
  (str (clojure.string/join ", " [street city state]) " " zip))

(defn -mk-html
  [biz-hits biz-cat-hits item-hits]
  (html
   [:div#auto_complete_list

    (if (not (empty? biz-cat-hits))
      [:h4.business_categories "Categories"])
    (if (not (empty? biz-cat-hits))
      [:ul.buinesss_categories
       (for [{id :_id {name :name} :_source} biz-cat-hits]
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
           [:span.address (-mk-addr-str address)]]])])
    
    [:ul
     [:li
      [:a#dropdown_submit_search {:href "#"} "Search all businesses"]]]]))

(defn -mk-biz-hit-response
  "From ES biz hit, make service hit."
  [{id :_id {n :name a :address} :_source}]
  {:id id :name n :address a})

(defn -mk-simple-hit-response
  "For ES hit of either biz-category or item, make a service hit."
  [{i :_id, {n :name} :_source}]
  {:id i :name n})

(defn -mk-res-map
  [f hits-map]
  {:count (:total hits-map)
   :hits  (map f (:hits hits-map))})

(defn -mk-arguments
  [query miles address lat lon size html?]
  {:query query
   :geo_filter {:miles miles :address address :lat lat :lon lon}
   :paging {:from 0 :size size}
   :html html?})

(defn -mk-response
  "From ES response, create service response."
  [{biz-hits-map :hits}
   {biz-cat-hits-map :hits}
   {item-hits-map :hits}
   query miles address lat lon size html?]
  {:endpoint "/v1/suggestions"   ; TODO: pass this in
   :arguments (-mk-arguments query miles address lat lon size html?)
   :results {:businesses
             (-mk-res-map -mk-biz-hit-response    biz-hits-map)
             :business_categories
             (-mk-res-map -mk-simple-hit-response biz-cat-hits-map)
             :items
             (-mk-res-map -mk-simple-hit-response item-hits-map)}})

(defn -mk-query
  "String 's' may contain mutiple terms.
   Perform a boolean query."
  [s]
  ;; split s into tokens.
  ;; take all but last token: create :term query for each.
  ;; take last token: create prefix match.
  (let [tokens (clojure.string/split s #" ")
        prefix (last tokens)
        fulls  (butlast tokens)]
    (let [foo (cons {:prefix {:name prefix}}
                    (map (fn [t] {:term {:name t}}) fulls))]
      {:bool {:must foo}})))

(defn -simple-search
  "Perform prefix search against names."
  [domain query from size]
  (let [es-names (domain cfg/elastic-search-names)]
    (es-doc/search (:index es-names) (:mapping es-names)
                   :query  (-mk-query query)
                   :from   from
                   :size   size)))

(defn validate-and-search
  "Perform 3 searches (in parallel!):
      - businesses (w/ filtering)
      - business_categories
      - items"
  [orig-query address orig-lat orig-lon miles size html]

  ;; Validate query.
  (let [query (q/normalize orig-query)]
    (if (clojure.string/blank? query)
      (validate/response-bad-query orig-query query)
      
      ;; Validate location info.
      (let [lat (inputs/str-to-val orig-lat nil)
            lon (inputs/str-to-val orig-lon nil)]
        (if (validate/invalid-location? address lat lon)
          (validate/response-bad-location address orig-lat orig-lon)
          
          ;; OK, make queries.
          (let [
                ;; transform params
                miles (inputs/str-to-val miles 4.0)
                {lat :lat lon :lon} (geo/get-lat-lon lat lon address)
                from  0
                size  (inputs/str-to-val size 5)
                html? (inputs/true-str? html)
                
                ;; fetch results
                ;; TODO: in *parallel*.  How?
                ;;  - pmap: Probably not worth the coordination overhead.
                ;;  - clojure.core.reducers/map
                ;;  - agents (uncoordinated, asynchronous)

                ;; biz-res     (biz-search/es-search query :prefix
                ;;                                   miles lat lon
                ;;                                   nil  ; -sort-
                ;;                                   from size)

                ;; FIXME FIXME FIXME
                ;; Need to make this a prefix search,
                ;; as in -simple-search
                biz-res     (biz-search/es-search query :match
                                                  miles lat lon
                                                  nil  ; -sort-
                                                  from size)

                biz-cat-res (-simple-search :business_categories query from size)
                item-res    (-simple-search :items query from size)]

            (responses/ok-json
             (if html?
               {:arguments (-mk-arguments query miles address lat lon size html?)
                :html (apply -mk-html (map #(:hits (:hits %))
                                           [biz-res biz-cat-res item-res]))}
               ;; Extract info from ES-results, create JSON response.
               (-mk-response biz-res biz-cat-res item-res
                             query miles address lat lon size html?)))))))))
