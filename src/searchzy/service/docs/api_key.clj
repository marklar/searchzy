(ns searchzy.service.docs.api-key
  (:use [hiccup.core]))

(defn api-key []
  (html
   [:h3 "API Key"]
   [:p "If you choose to add an " [:span.code "api-key"] " to your "
    ".config.yaml, then you'll need to pass the same value in your "
    [:span.code "api_key"] " query-string parameter with every request. "
    "Please note: even though the config file uses a dash, "
    "this (and all query-string parameters) uses an UNDERBAR."]
   [:ul
    [:li "name: " [:span.code "api_key"]]
    [:li "type: string"]
    [:li "optional - needed only when the server requires an API key."]]))
