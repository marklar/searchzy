(ns searchzy.service.docs.hours
  (:use [hiccup.core]))

(defn old-filtering []
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

(defn filtering []
  (html
   [:h3 "Filtering: Business Hours"]
   [:p "Allows one to specify a day (of the week) and time. "
    "Only businesses open then are returned."]
   [:p "For many businesses, we lack information about hours of operation. "
    "For filtering purposes, those businesseses will always "
    "be considered closed (and thus filtered out)."]
   [:ul
    [:li "name: " [:span.code "hours"]]
    [:li "optional - if absent, no filtering is performed"]
    [:li "type: formatted string"]
    [:ul
     [:li "format: " [:span.code "day-of-week/hour:minute"]]
     [:li "values: "]
     [:ul
      [:li "day-of-week: integer in range " [:span.code "[0..6]"]]
      [:li "hour: integer in range " [:span.code "[0..23]"]]
      [:li "minute: integer in range " [:span.code "[0..59]"]]]
     [:li "e.g.: " [:span.code "0/19:30"] " means Sunday at 7:30pm"]]]))
