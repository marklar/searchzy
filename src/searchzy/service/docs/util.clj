(ns searchzy.service.docs.util)

(def lat 40.714)
(def lon -74.006)
(def address "New%20York,%20NY")
(def miles 2.5)

(def style-str
  (str "body {font-family: Verdana}"
       ".code {font-family: \"Courier New\"; font-weight: bold}"
       ".i  {font-style: italic}"
       "h2 {color: blue}"
       "h3 {color: green}"))

(defn- mk-query-str
  [params]
  (clojure.string/join "&"
                       (map
                        (fn [[k v]] (str (name k) "=" v))
                        params)))

;; TODO: make this take "& keyvals" instead of an explicit hash.
;; Nicer for passing args.
;; e.g. (mk-url path :query query :address address :miles miles :size size)
(defn mk-url [path params]
  (str path "?" (mk-query-str params)))

