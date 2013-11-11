(ns searchzy.service.docs.query
  (:use [hiccup.core]))

(defn query
  [model-name]
  (html
   [:h3 "Query"]
   [:ul
    [:li "name: " [:span.code "query"]]
    [:li "type: string"]
    [:li "required"]
    [:li "purpose: for searching against names of " model-name]
    [:li "interpretation:"
     [:ul
      [:li "expected to be complete words (not just prefixes)"]
      [:li "treated as case-insensitive"]]]]))

