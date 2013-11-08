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

(defn -json-p-response
  [status obj]
  (let [r (-json-response status obj)]
    (assoc r :body (-json-p (:body r)))))

(defn ok-json
  [obj]
  ;; (-json-response 200 obj))
  (-json-p-response 200 obj))

;; (defn ok-json-p
;;   [obj]
;;   (-json-p-response 200 obj))

(defn error-json
  [obj]
  ;; (-json-response 404 obj))
  (-json-p-response 404 obj))

;; (defn error-json-p
;;   [obj]
;;   (-json-p-response 404 obj))
