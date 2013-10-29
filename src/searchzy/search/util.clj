(ns searchzy.search.util
  (:require [geocoder.google :as geo]       ;; #{bing geonames google}
            [clojure.string]
            [clojure.data.json :as json]))

(defn str-to-val
  "Taking an HTTP query parameter and convert it to a proper value,
   using a default value if none provided."
  [str default]
  (if (clojure.string/blank? str)
    default
    (let [val (read-string str)]
      (if (nil? val)
        default
        val))))

(defn get-geolocation
  "Given an address (e.g. '2491 Aztec Way, Palo Alto, CA 94303'),
   return a map with keys :lat, :lng."
  [address]
  ;; -- Makes remote call. --
  (:location (:geometry (first (geo/geocode-address address)))))

(defn get-lat-lng
  "If lat,lng are good, just return as map.
   Otherwise use address to look up geocoordinates."
  [lat lng address]
  (if (or (nil? lat) (nil? lng))
    (get-geolocation address)
    {:lat lat :lng lng}))

(defn true-str?
  "Convert string (e.g. from cmd-line or http params) to bool."
  [s]
  (contains? #{"true" "t" "1"} s))

(def json-headers
  {"Content-Type" "application/json; charset=utf-8"})

(defn json-response
  [status obj]
  {:status status
   :headers json-headers
   :body (json/write-str obj)})

(defn ok-json-response
  [obj]
  (json-response 200 obj))

(defn error-json-response
  [obj]
  (json-response 404 obj))
  
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
