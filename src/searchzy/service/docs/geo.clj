(ns searchzy.service.docs.geo
  (:use [hiccup.core]))

(defn geo-filter []
  (html
   [:h3 "Filtering: Geographic Distance"]

   [:h4 "Distance (Radius)"]
   [:ul
    [:li "name: " [:span.code "miles"]]
    [:li "type: float"]
    [:li "optional: defaults to " [:span.code "4.0"]]
    [:li "purpose: Define the radius (in miles) for the proximity filter."]]
   
   [:h4 "Location"]
   [:p "Filter results by proximity to this location."]
   [:p [:span.i "Required"] ": either "
    [:span.code "address"]
    " -or- both "
    [:span.code "lat"] " and " [:span.code "lon"] "."]

   ;; address
   [:ul
    [:li "address"
     [:ul
      [:li "name: " [:span.code "address"]]
      [:li "type: string"]
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

   ;; lat, lon
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
         [:li "e.g. " [:span.code "40.278171872"]]]]]]]]))
