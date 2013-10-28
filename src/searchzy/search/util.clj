(ns searchzy.search.util
  (:require [clojure.data.json :as json]))


(defn true-str?
  "Convert string to bool."
  [s]
  (contains? #{"true" "t" "1"} s))

(def json-headers
  {"Content-Type" "application/json; charset=utf-8"})

(defn ok-json-response
  [obj]
  {:status 200
   :headers json-headers
   :body (json/write-str obj)})
  
;; To perform a query with Elastisch, use the
;; clojurewerkz.elastisch.rest.document/search function. It takes
;; index name, mapping name and query (as a Clojure map):
;;
;;
;; (ns foo
;;   (:require [clojurewerkz.elastisch.native          :as es]
;;             [clojurewerkz.elastisch.native.document :as es-doc]
;;             [clojurewerkz.elastisch.query           :as es-q]
;;             [clojurewerkz.elastisch.native.response :as es-rsp]
;;             [clojure.pprint :as pp]))
;;
;; (let [res  (es-doc/search "myapp_development" "person"
;;                           :query (es-q/term :biography "New York"))
;;       n    (es-rsp/total-hits res)
;;       hits (es-rsp/hits-from res)]
;;   (println (format "Total hits: %d" n))
;;   (pp/pprint hits)))
;;
