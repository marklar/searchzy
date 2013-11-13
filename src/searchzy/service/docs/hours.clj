(ns searchzy.service.docs.hours
  (:use [hiccup.core]))

(defn filtering []
  (html
   [:h3 "Filtering: Business Hours"]
   [:p "Filtering by business hours is " [:span.i "optional"] ". "]
   [:p "Either " [:span.i "all three"]
    " of these arguments should be present, or " [:span.i "none"] ". "
    "If a non-zero subset is present, it's treated as an error."]
   [:ul
    [:li "day of week"
     [:ul
      [:li "name: " [:span.code "day"]]
      [:li "type: integer " [:span.code "[0..6]"]]
      [:li "Sunday: " [:span.code "0"]]]]
    [:li "hour of day"
     [:ul
      [:li "name: " [:span.code "hour"]]
      [:li "type: integer " [:span.code "[0..23]"]]]]
    [:li "minute"
     [:ul
      [:li "name: " [:span.code "minute"]]
      [:li "type: integer " [:span.code "[0..59]"]]]]]))
