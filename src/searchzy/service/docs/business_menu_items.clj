(ns searchzy.service.docs.business-menu-items
  (:use [hiccup.core])
  (:require [searchzy.service.docs
             [util :as util]
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
       [:li [:a {:href url2} url2]]]])))
