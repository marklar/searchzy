(ns searchzy.service.suggestions.response
  "For suggestions searches."
  (:use [hiccup.core])
  (:require [searchzy.service
             [util :as util]]))

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

;;
;; TODO: Include more info for each business:
;;   - coordinates
;;   - open/close time for today
;;
;; {
;;   id: "4fec6cd605b3c53e3800cccd",
;;   fdb_id: "f4431f27-755f-b8b9-4001-0141d919724d",
;;   name: "Nail Concepts",
;;   address: {
;;     zip: "07030",
;;     street: "633 Willow Ave",
;;     state: "NJ",
;;     city: "Hoboken"
;;   },
;;   business_category_ids: [
;;     "4fb4706bbcd7ac45cf000019",
;;     "5076e7386bddbbdc42000001",
;;     "4fb4706abcd7ac45cf000012"
;;   ]
;; }


;; SOURCE:
;; {
;;   :business_category_ids [4fb4706bbcd7ac45cf000019 5076e7386bddbbdc42000001 ...],
;;   :rails_time_zone Eastern Time (US & Canada),
;;   :yelp_id polish-nail-new-york,
;;   :coordinates {:lon -73.97136, :lat 40.69319},
;;   :yelp_star_rating 5.0,
;;   :name Polish Nail,
;;   :yelp_review_count 1,
;;   :hours [
;;     { :wday 0,
;;       :hours {:open {:minute 0, :hour 10},
;;               :close {:minute 0, :hour 19}}
;;     }
;;     ...
;;   :permalink polish-nail-new-york-ny-1,
;;   :fdb_id f5c71225-6dcc-b8b9-4001-0141dc524cab,
;;   :latitude_longitude 40.69319,-73.97136,
;;   :phone_number 1-718-5966328,
;;   :value_score_int 0,
;;   :address {
;;     :zip 11205,
;;     :street 361 Myrtle Ave,
;;     :state NY, :city Brooklyn
;;   }
;; }

(defn- mk-biz-hit-response
  "From ES biz hit, make service hit."
  [{id :_id, source :_source}, day-of-week]
  (let [hours-today (util/get-hours-for-day (:hours source) day-of-week)]
    {:id id
     :fdb_id (:fdb_id source)
     :name (:name source)
     :address (:address source)
     :business_category_ids (:business_category_ids source)
     ;; new
     :hours_today hours-today
     :coordinates (:coordinates source)
     }))

(defn- mk-biz-cat-hit-response
  "For ES hit biz-category, make a service hit."
  [{i :_id, {n :name, fdb_id :fdb_id} :_source}]
  {:id i, :fdb_id fdb_id, :name n})

(defn- mk-item-hit-response
  "For ES item hit, make a service hit."
  [{i :_id, {n :name, bcis :business_category_ids, fdb_id :fdb_id} :_source}]
  {:id i, :fdb_id fdb_id, :name n, :business_category_ids bcis})

(defn- mk-res-map
  [f hits-map]
  {:count (:total hits-map)
   :hits  (map f (:hits hits-map))})

;; Unused?
(defn- mk-arguments
  [query geo-map page-map html?]
  {:query query
   :geo_filter geo-map
   :paging page-map
   :html html?})

;;---------------------------

;; We don't have an hours-map or a utc-offset-map,
;; so we merely GUESS at the current day.

(defn mk-response
  "From ES response, create service response."
  [biz-res cat-res item-res endpoint query biz-cat-ids
   geo-map page-map utc-offset-map html?]
  (let [tmp {:arguments {:query query
                         :business_category_ids biz-cat-ids
                         :utc_offset utc-offset-map
                         :geo_filter geo-map
                         :paging page-map
                         :html html?}
             :endpoint endpoint}
        day-of-week (util/get-day-of-week-from-tz
                     (some #(-> % :_source :business :rails_time_zone) biz-res)
                     utc-offset-map)]

    (merge
     (if html?
       {:html (apply mk-html (map :hits [biz-res cat-res item-res]))}
       {:results {:businesses
                  (mk-res-map #(mk-biz-hit-response % day-of-week) biz-res)
                  :business_categories
                  (mk-res-map mk-biz-cat-hit-response cat-res)
                  :items
                  (mk-res-map mk-item-hit-response item-res)}})
     tmp)))
