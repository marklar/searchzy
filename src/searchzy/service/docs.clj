(ns searchzy.service.docs
  (:use [hiccup.core]))

(defn suggestions []
  (let [path "/v1/suggestions?"
        query "nai"
        lat 40.7143528
        lon -74.00597309999999
        address "New%20York,%20NY"
        miles 2.5
        size 8
        url1 (str path
                  "query=" query
                  "&address=" address
                  "&miles=" miles
                  "&size=" size)
        url2 (str path
                  "query=" query
                  "&lat=" lat
                  "&lon=" lon)
        url3 (str url2 "&html=true")]
    (html
     [:head
      [:title "Searchzy :: Suggestions"]
      [:style "body {font-family: Verdana}
             .code {font-family: \"Courier New\"; font-weight: bold}"]]
     [:body
      
      [:h1 "Searchzy Auto-Suggest API"]
      
      [:h2 "Try It"]
      [:p "First, try a couple of queries. "
       "In the results, be sure to have a look at the "
       [:span.code "arguments"] ", to get a sense for how it's interpreting the query-string parameters."]

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

      ;; Section
      [:h2 "Behavior"]
      [:h3 "Query"]
      [:p "This is an auto-suggest query.  The final token of a user’s query is treated as a prefix.  For example, \"hai\" might be the beginning of the words \"hair\" or \"haircut\"."]
      
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
      
      ;; Section
      [:h2 "Endpoint"]
      [:p "The path: "
       [:span.code "/v1/suggestions"]]
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
         [:li "When " [:span.code "true"] ", outputs JSON with single attribute " [:span.code "html"] ", an HTML string ready to be injected into the DOM."]
         [:li "When " [:span.code "false"] ", outputs all data as JSON."]]]]
      
      [:h3 "Prefix Query"]
      [:ul
       [:li "name: " [:span.code "query"]]
       [:li "type: String"]
       [:li "required"]
       [:li "purpose: for searching against the names of:"
        [:ul
         [:li "BusinessCategories"]
         [:li "Items"]
         [:li "Businesses"]]
        [:li "interpretation:"
         [:ul
          [:li "initial tokens are treated as complete words"]
          [:li "the final token is treated as a prefix"]
          [:li "treated as case-insensitive"]]]]]
      
      [:h3 "Geographic Filter"]

      [:h4 "Distance"]
      [:ul
       [:li "name: " [:span.code "miles"]]
       [:li "type: float"]
       [:li "optional: defaults to " [:span.code "4.0"]]
       [:li "purpose: Define the radius (in miles) for the proximity filter."]]
      
      ;; [:h4 "Distances"]
      ;; [:p "Define the radii (in miles) for the proximity filters."]
      ;; [:ul
      ;;  [:li "shorter distance"
      ;;   [:ul
      ;;    [:li "name: " [:span.code "min_radius"]]
      ;;    [:li "type: float"]
      ;;    [:li "default: 1.0  (miles)"]]]
      ;;  [:li "longer distance"
      ;;   [:ul
      ;;    [:li "name: " [:span.code "max_radius"]]
      ;;    [:li "type: float"]
      ;;    [:li "default: 5.0  (miles)"]]]]
      
      [:h4 "Location"]
      [:p "Filter results by proximity to this location."]
      [:p "Required: either "
       [:span.code "address"]
       " -or- both "
       [:span.code "lat"] " and " [:span.code "lon"] "."]
      [:ul
       [:li "address"
        [:ul
         [:li "name: " [:span.code "address"]]
         [:li "type: String"]
         [:li "the value can take many different forms, such as:"
          [:ul
           [:li "just a zip: " [:span.code "94303"]]
           [:li "city, state: " [:span.code "New York, NY"]]
           [:li "a full street address: " [:span.code "123 Main St., New York, NY"]]]
         [:li "notes:"
          [:ul
           [:li "Input to a geo-coding service (Google or Bing)."]
           [:li "To be used only if either "
            [:span.code "lat"]
            " or "
            [:span.code "lon"]
            " is absent."]]]]]]]
      
      [:ul
       [:li "latitude AND longitude"
        [:ul
         [:li "latitude"
          [:ul
           [:li "name: " [:span.code "lat"]]
           [:li "type: float"]
           [:li "e.g. " [:span.code "10.347372387"]]]
          [:li "longitude"
           [:ul
            [:li "name: " [:span.code "lon"]]
            [:li "type: float"]
            [:li "e.g. " [:span.code "40.278171872"]]]]]]]]])))

(defn show []
  (html
   [:h1 "Searchzy API"]
   
   [:p "There are three endpoints.  All are GETs."]
   [:ul
    [:li
     [:a {:href "/docs/suggestions"} "/v1/suggestions"]]
    [:li "/v1/business_menu_items"]
    [:li "/v1/businesses"]]))
