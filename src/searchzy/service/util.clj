(ns searchzy.service.util
  (:require [clojure.data.json :as json]))

;; TODO: rename this namespace to 'responses' or somesuch.

(def -json-headers
  {"Content-Type" "application/json; charset=utf-8"})

(defn -json-response
  [status obj]
  {:status status
   :headers -json-headers
   :body (json/write-str obj)})

(defn ok-json-response
  [obj]
  (-json-response 200 obj))

(defn error-json-response
  [obj]
  (-json-response 404 obj))
