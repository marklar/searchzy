(ns searchzy.service.docs.suggestions
  (:use [hiccup.core])
  (:require [searchzy.service.docs
             [util :as util]
             [geo :as geo]]))

(defn show []
  (let [path "/v1/suggestions"
        query "nai"
        size 8
        url1 (util/mk-url path {:query query
                                :address util/address
                                :miles util/miles
                                :size size})
        url2 (util/mk-url path {:query query
                                :lat util/lat
                                :lon util/lon})
        url3 (str url2 "&html=true")
        url4 (util/mk-url path {:query query :lat util/lat})]
    (html
     [:head
      [:title "Searchzy :: Suggestions"]
      [:style util/style-str]]

     [:body
      
      [:h1 "Searchzy Auto-Suggest"]
      
      [:h2 "Try It"]

      [:p "First, try a couple of queries. "
       "In the results, be sure to have a look at the "
       [:span.code "arguments"] 
       ", to get a sense for how it's interpreting the query-string parameters."]

      [:h3 "Well-formed Queries"]
      [:p "Using " [:span.code "address"]
        " and specifying some optional values, "
       [:span.code "miles"] " and " [:span.code "size"] ": "]
      [:ul
       [:li [:a {:href url1} url1]]]

      [:p "Using " [:span.code "lat"] " and " [:span.code "lon"]
       " instead of " [:span.code "address:"]]
      [:ul
       [:li [:a {:href url2} url2]]]

      [:p "Same thing, but this time with " [:span.code "html"] " output:"]
      [:ul
       [:li [:a {:href url3} url3]]]

      [:p "When there's no error, the response code is " [:span.code "200"] "."]

      [:h3 "Errors"]
      [:p "Try a malformed query, to see what an error looks like:"]
      [:ul
       [:li [:a {:href url4} url4]]]

      [:p "If you look at the response headers, "
       "you can see that it returns a " [:span.code "404"] ":"]
      [:ul
       [:li [:span.code "curl -I \"http://localhost:3000/v1/suggestions?query=nai&lat=40.7143528\""]]]

      ;; Section
      [:h2 "Behavior"]
      [:h3 "Query"]
      [:p "This is an auto-suggest query.  The final token of a user’s query "
       "is treated as a prefix.  For example, \"hai\" might be the beginning "
       "of the words \"hair\" or \"haircut\"."]
      
      [:p "The search services performs prefix queries against three domains:"]
      [:ul
       [:li "BusinessCategory names - e.g. \"Hair Salon\""]
       [:li "Item names - e.g. \"Men's haircut\""]
       [:li "Business names - e.g. \"Cinderella Hair Castle\""]]
      
      [:h3 "Filtering"]
      [:p "For Businesses, filtered by geographic proximity to the provided location."]
      [:p "BusinessCategory names and Item names are not filtered."]
      
      [:h3 "Sorting"]
      [:p "By ElasticSearch’s lexical sorting."]
      
      [:h3 "Paging"]
      [:p "Returns only the first 'page' of results for each domain, 5 by default."]

      [:h3 "Format"]
      [:p "The response is always JSON.  Within the JSON response, "
       "you may choose to have the data embedded as either:"]
      [:ul
       [:li "JSON data, -OR-"]
       [:li "an HTML string (to be injected into the DOM)."]]
      
      ;; Section
      [:h2 "Endpoint"]
      [:p "The path: " [:span.code "/v1/suggestions"]]
      [:p "It requires query string parameters to work correctly."]
      
      ;; Section
      [:h2 "Query String Parameters"]
      
      [:h3 "Output Format"]
      [:ul
       [:li "name: " [:span.code "html"]]
       [:li "type: boolean"
        [:ul
         [:li "true: any of " [:span.code "[true, t, 1]"]]
         [:li "false: anything else"]]]
       [:li "optional: defaults to " [:span.code "false"]]
       [:li "purpose:"
        [:ul
         [:li "When " [:span.code "true"] ", outputs JSON with single attribute "
          [:span.code "html"] ", an HTML string ready to be injected into the DOM."]
         [:li "When " [:span.code "false"] ", outputs all data as JSON."]]]]
      
      [:h3 "Prefix Query"]
      [:ul
       [:li "name: " [:span.code "query"]]
       [:li "type: string"]
       [:li "required"]
       [:li "purpose: for searching against the names of:"
        [:ul
         [:li "BusinessCategories"]
         [:li "Items"]
         [:li "Businesses"]]]
       [:li "interpretation:"
        [:ul
         [:li "initial tokens are treated as complete words"]
         [:li "the final token is treated as a prefix"]
         [:li "treated as case-insensitive"]]]]

      (geo/geo-filter)
      ])))
