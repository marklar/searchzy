(ns searchzy.service.responses
  (:require [clojure.data.json :as json]))

(def -json-headers
  {"Content-Type" "application/json; charset=utf-8"})

(defn -json-p
  [json]
  (str "jsonCallBack(" json ")"))

(defn -json-response
  [status obj]
  {:status status
   :headers -json-headers
   :body (json/write-str obj)})

;; -- public --

(defn p-ify
  "Ring middleware!"
  [resp]
  (assoc resp :body (-json-p (:body resp))))

;; -- ok --

(defn ok-json
  [obj]
  (-json-response 200 obj))

(defn ok-json-p
  [obj]
  (p-ify (ok-json obj)))

;; -- error --

(defn error-json
  [obj]
  (-json-response 404 obj))

(defn error-json-p
  [obj]
  (p-ify (error-json)))
