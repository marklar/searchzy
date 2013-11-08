(ns searchzy.service.docs.core
  (:use [hiccup.core])
  (:require [searchzy.service.docs
             [util :as util]
             [geo :as geo]]))


(defn show []
  (html
   [:head
    [:title "Searchzy :: Businesses"]
    [:style util/style-str]]
   [:body
    [:h1 "Searchzy API"]
    
    [:p "There are three endpoints.  All are GETs."]
    [:ul
     [:li
      [:a {:href "/docs/suggestions"} [:span.code "/v1/suggestions"]]
      [:ul
       [:li "for each keystroke"]
       [:li "matches names of business_categories, items, and businesses"]]]

     [:li
      [:a {:href "/docs/business_menu_items"} [:span.code "/v1/business_menu_items"]]
      [:ul
       [:li "for when one selects either a suggested business_category or a suggested item"]
       [:li "matches menu_items for the selected item_id"]]]

     [:li
      [:a {:href "/docs/businesses"} [:span.code "/v1/businesses"]]
      [:ul
       [:li "for when one does not select a suggestion"]
       [:li "searches over only business names"]]]]]))
