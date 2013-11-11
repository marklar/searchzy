(ns searchzy.service.docs.businesses
  (:use [hiccup.core])
  (:require [searchzy.service.docs
             [util :as util]
             [geo :as geo]]))

(defn show []
  (let [path "/v1/businesses"
        query "nails"
        from 0
        size 8
        url1 (util/mk-url path {:query query
                                :address util/address
                                :miles util/miles
                                :from from :size size})
        url2 (util/mk-url path {:query query
                                :lat util/lat :lon util/lon})]
    (html
     [:head
      [:title "Searchzy :: Businesses"]
      [:style util/style-str]]
     [:body
      
      [:h1 "Searchzy Businesses"]
      
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
      [:p "The user's query (e.g. \"curves\") is interpreted as "
       "(part of) the name of a Business, such as \"Curves Gym\".  "
       "The purpose of this endpoint is to find those Businesses "
       "within a geographical area whose names are the closest match "
       "to the query string."]
      [:p "This is not a prefix search.  It happens as a result of "
       "the user submitting the form (i.e. clicking \"search\")."]
      
      [:h3 "Filtering"]
      [:p "By geographical proximity to a geo-point."]
      
      [:h3 "Sorting"]
      [:p "Two options:"]
      [:ul
       [:li "By ElasticSearch’s lexical sorting.   -OR-"]
       [:li "FIXME: By \"value_score_int\", which is ElasticSearch’s score "
        "PLUS the highest value of any of a Business’s BusinessItems."]]
      
      [:h3 "Paging"]
      [:p "Only a subset of results is returned, based on provided offsets."]
      
      [:h3 "Results"]
      [:p "The returned data for each Business includes its hours of "
       "operation for the current day of the week. This is inconvenient "
       "because it makes the query referentially non-transparent.  "
       "That is, the same query on different days of the week may "
       "produce different results.  Such behavior makes it difficult to "
       "cache the results."]
      [:p "Perhaps it would be better simply to return "
       "the hours of operation for every day of the week and allow the "
       "client to decide how to use that data."]
      
      [:h2 "Endpoint"]
      [:p "The path: " [:span.code "/v1/businesses"]]
      [:p "It requires query string parameters to work correctly."]
      
      
      [:h2 "Query String Parameters"]
      
      [:h3 "Query"]
      [:ul
       [:li "name: " [:span.code "query"]]
       [:li "type: string"]
       [:li "required"]
       [:li "purpose: for searching against names of Businesses"]
       [:li "interpretation:"
        [:ul
         [:li "expected to be complete words (not just prefixes)"]
         [:li "treated as case-insensitive"]]]]
      
      [:h3 "Paging"]
      [:ul
       [:li "start index"
        [:ul
         [:li "name: " [:span.code "from"]]
         [:li "type: integer"]
         [:li "optional: defaults to " [:span.code "0"]]
         [:li "notes:"
          [:ul
           [:li "indices start at " [:span.code "0"]]
           [:li "If there are fewer than " [:span.code "from"]
            " results, return none."]]]]]
       [:li "number to include"
        [:ul
         [:li "name: " [:span.code "size"]]
         [:li "type: integer"]
         [:li "optional: defaults to " [:span.code "10"]]
         [:li "note: If there are fewer than " [:span.code "size"]
          " available, just return those."]]]]
      
      [:h3 "Sort"]
      [:ul
       [:li "name: " [:span.code "sort"]]
       [:li "type: enum " [:span.code "[value, lexical]"]]
       [:li "optional: defaults to " [:span.code "value"]]
       [:li "validation: If a non-legal value for sort is provided, "
        "the service will use the default (i.e. " [:span.code "value"] ")."]]
      
      (geo/geo-filter)
      ])))