(ns searchzy.service.docs.paging
  (:use [hiccup.core]))

(defn paging []
  (html
   [:h3 "Paging"]
   [:ul
    [:li "start index"
     [:ul
      [:li "name: " [:span.code "from"]]
      [:li "type: integer"]
      [:li "optional: defaults to " [:span.code "0"]]
      [:li "notes:"
       [:ul
        [:li "Indices start at " [:span.code "0"] "."]
        [:li "If there are fewer than " [:span.code "from"]
         " results, return " [:span.code "[]"]"."]]]]]
    [:li "number to include"
     [:ul
      [:li "name: " [:span.code "size"]]
      [:li "type: integer"]
      [:li "optional: defaults to " [:span.code "10"]]
      [:li "note: If there are fewer than " [:span.code "size"]
       " available, just return those."]]]]))
