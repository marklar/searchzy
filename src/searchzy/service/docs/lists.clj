(ns searchzy.service.docs.lists
  (:use [hiccup.core])
  (:require [searchzy.service.docs
             [api-key :as api-key]
             [util :as util]
             [paging :as paging]
             [geo :as geo]]))


(defn show []
  (let [path "/v1/lists"
        from 0
        size 8
        url1 (util/mk-url path {:address util/address
                                :miles util/miles
                                :from from :size size})
        url2 (util/mk-url path {:lat util/lat :lon util/lon})]
    (html
     [:head
      [:title "Searchzy :: Businesses"]
      [:style util/style-str]]
     [:body
      
      [:h1 "Searchzy Lists -- FIX ME!!!"]
      
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
       [:li [:a {:href url1} [:span.code url1]]]]

      [:p "Using " [:span.code "lat"] " and " [:span.code "lon"]
       " instead of " [:span.code "address:"]]
      [:ul
       [:li [:a {:href url2} [:span.code url2]]]]

      [:h2 "Behavior"]
      
      [:h3 "Filtering"]
      [:p "By geographical proximity to a geo-point."]
      
      [:h3 "Paging"]
      [:p "Only a subset of results is returned, based on provided offsets."]
      
      [:h3 "Results"]
      [:p "The returned data is simply Lists."]
      
      [:h2 "Endpoint"]
      [:p "The path: " [:span.code "/v1/lists"]]
      [:p "It requires query-string parameters to work correctly."]
      
      
      [:h2 "Query String Parameters"]

      [:p "TODO: Add stuff here."]

      (api-key/api-key)
      (paging/paging)
      (geo/geo-filter)
      ])))
