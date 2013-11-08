(ns searchzy.service.responses
  (:require [clojure.data.json :as json]))

(def -json-headers
  {"Content-Type" "application/json; charset=utf-8"})

(defn -json-response
  [status obj]
  {:status status
   :headers -json-headers
   :body (json/write-str obj)})

;; -- public --

(defn json-p-ify
  "Ring middleware!
   Take a response map.  Return w/ body wrapped in JSONP."
  [{b :body :as resp}]
  (assoc resp :body (str "jsonCallBack(" b ")")))

(defn ok-json
  [obj]
  (-json-response 200 obj))

(defn error-json
  [obj]
  (-json-response 404 obj))
