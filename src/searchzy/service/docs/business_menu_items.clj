(ns searchzy.service.docs.business-menu-items
  (:use [hiccup.core])
  (:require [searchzy.service.docs
             [util :as util]
             [query :as query]
             [paging :as paging]
             [geo :as geo]]))

(defn show []
  (let [path "/v1/business_menu_items"
        item_id "5076ea696bddbbdcc000000e"
        from 0
        size 8
        url1 (util/mk-url path {:item_id item_id
                                :address util/address
                                :miles util/miles
                                :from from :size size})
        url2 (util/mk-url path {:item_id item_id
                                :lat util/lat :lon util/lon})]
    (html
     [:head
      [:title "Searchzy :: BusinessMenuItems"]
      [:style util/style-str]]
     [:body
      [:h1 "Searchzy BusinessMenuItems"]
      
      [:h2 "Try It"]
      [:p "First, try a couple of queries. "
       "In the results, be sure to have a look at the "
       [:span.code "arguments"]
       ", to get a sense for how it's interpreting the query-string parameters."]
      
      [:p "Using " [:span.code "address"]
       " and specifying some optional values, "
       [:span.code "miles"] ", " [:span.code "from"]
       ", and " [:span.code "size"] ": "]
      [:ul
       [:li [:a {:href url1} url1]]]
      
      [:p "Using " [:span.code "lat"] " and " [:span.code "lon"]
       " instead of " [:span.code "address:"]]
      [:ul
       [:li [:a {:href url2} url2]]]
      
      [:h2 "Behavior"]
      
      [:h3 "Query"]
      [:p "The user selects a BusinessCategory from an auto-suggest list.  "
       "Then either the user or the application selects one Item from that "
       "BusinessCategory.  The application then queries for local Businesses "
       "which contain a BusinessUnifiedMenuItem of that Item."]
      
      [:p "For example, the user selects the BusinessCategory \"Hair Salons\", "
       "and the application selects the default Item \"Men's Haircut\".  "
       "The app queries Searchzy, which returns information about local "
       "Businesses that provide \"Men's Haircut\"."]
      
      [:h3 "Results"]
      [:p "Each result in the search results could be thought of as a "
       "\"Business + MenuItem\".  It combines information about the "
       "Business provider (e.g. its name, location, hours of operation, "
       "etc.) and the MenuItem itself (i.e. its price)."]
      
      [:p "Searchzy also provides aggregate information about those "
       "\"Business + MenuItem\" combos, including until what time the "
       "last relevant Business is open, and what the min, max, and mean "
       "prices are for all MenuItems."]
      
      [:h3 "Filtering"]
      [:p "Of Businesses, by geographical proximity to a geo-point."]
      
      [:h3 "Sorting"]
      [:p "By value_score_picos of the MenuItem."]
      
      [:h3 "Paging"]
      [:p "Only a subset of results is returned, based on provided offsets."]
      
      [:h2 "Endpoint"]
      [:p "The path: " [:span.code "/v1/business_menu_items"]]
      [:p "It requires query-string parameters to work correctly."]
      
      [:h2 "Query String Parameters"]
      
      (query/query "Businesses")
      (paging/paging)
      (geo/geo-filter)
      ])))
