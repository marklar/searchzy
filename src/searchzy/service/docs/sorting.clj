(ns searchzy.service.docs.sorting
  (:use [hiccup.core]))

(defn biz-menu-items []
  (html
   [:h3 "Sorting"]
   [:ul
    [:li "name: " [:span.code "sort"]]
    [:li "type: enum " [:span.code "{price, -price, value, -value, distance, -distance}"]]
    [:li "optional: defaults to " [:span.code "-value"]]
    [:li "order:"
     [:ul
      [:li "A '" [:span.code "-"] "' prefix means DESC order. "
       "(You'll probably want to use " [:span.code "-value"] ")."]
      [:li "No prefix means ASC order. "
       "(That's probably what you'll want to use for "
       [:span.code "distance"] " and " [:span.code "price"] ".)"]]]]))


(defn businesses []
  (html
   [:h3 "Sorting"]
   [:ul
    [:li "name: " [:span.code "sort"]]
    [:li "type: enum "
     [:span.code "{value, -value, distance, -distance, score, -score}"]]
    [:li "optional: defaults to " [:span.code "-value"]]
    [:li "order:"
     [:ul
      [:li "A '" [:span.code "-"] "' prefix means DESC order. "
       "(You'll probably want to use " [:span.code "-value"]
       " and " [:span.code "-score"] ".)"]
      [:li "No prefix means ASC order. "
       "(You'll probably want to use " [:span.code "distance"] ".)"]]]]))

