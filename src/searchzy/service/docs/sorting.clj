(ns searchzy.service.docs.sorting
  (:use [hiccup.core]))

(defn biz-menu-items []
  (html
   [:h3 "Sorting"]
   [:ul
    [:li "name: " [:span.code "sort"]]
    [:li "type: enum " [:span.code "{+price, -price, +value, -value, +distance, -distance, +score, -score}"]]
    [:li "optional: defaults to " [:span.code "-value"]]
    [:li "direction:"
     [:ul
      [:li "A '" [:span.code "+"] "' prefix means " [:span.i "ascending"]
       ". (You'll probably want to use " [:span.code "+distance"] ".)"]
      [:li "A '" [:span.code "-"] "' prefix means " [:span.i "descending"]
       ". (You'll probably want to use " [:span.code "-value"]
       ", " [:span.code "-price"] " and " [:span.code "-score"] ".)"]]]
    [:li "validation: If a non-legal value for sort is provided, "
     "the service will use the default (i.e. " [:span.code "-value"] ")."]]))


(defn businesses []
  (html
   [:h3 "Sorting"]
   [:ul
    [:li "name: " [:span.code "sort"]]
    [:li "type: enum "
     [:span.code "{+value, -value, +distance, -distance, +score, -score}"]]
    [:li "optional: defaults to " [:span.code "-value"]]
    [:li "direction:"
     [:ul
      [:li "A '" [:span.code "+"] "' prefix means " [:span.i "ascending"]
       ". (You'll probably want to use " [:span.code "+distance"] ".)"]
      [:li "A '" [:span.code "-"] "' prefix means " [:span.i "descending"]
       ". (You'll probably want to use " [:span.code "-value"]
       " and " [:span.code "-score"] ".)"]]]
    [:li "validation: If a non-legal value for sort is provided, "
     "the service will use the default (i.e. " [:span.code "-value"] ")."]]))

