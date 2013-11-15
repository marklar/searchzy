(ns searchzy.service.inputs
  (:require [clojure.string :as str]
            [searchzy.service [geo :as geo]]))

;; TODO: Use (Integer. s) ?
(defn str-to-val
  "Taking an HTTP query parameter and convert it to a proper value,
   using a default value if none provided."
  [str default]
  (if (str/blank? str)
    default
    (or (read-string str) default)))

(defn true-str?
  "Convert string (e.g. from cmd-line or http params) to bool."
  [s]
  (contains? #{"true" "t" "1"} s))

(defn mk-page-map
  "From input info, create usable info."
  [{:keys [from size]}]
  {:from (str-to-val from 0)
   :size (str-to-val size 10)})

(defn mk-geo-map
  "Take input-geo-map: miles, address, lat, lon.
   If the input is valid, create a geo-map.
   If not, return nil."
  [{address :address, lat-str :lat, lon-str :lon, miles-str :miles}]
  (let [lat   (str-to-val lat-str   nil)
        lon   (str-to-val lon-str   nil)
        miles (str-to-val miles-str 4.0)]
    (let [res (geo/resolve-address lat lon address)]
      (if (nil? res)
        nil
        {:address {:input address, :resolved (:address res)}
         :coords (:coords res)
         :miles miles}))))

;; ---

(defn- get-day-hour-minute
  [input-map]
  (let [hours-str (:hours input-map)]
    (if (not (clojure.string/blank? hours-str))
      ;; from combo-string
      (clojure.string/split hours-str #"[^\d]")
      ;; from separate strings
      (let [{:keys [wday hour minute]} input-map]
        [wday hour minute]))))

(defn get-hours-map
  ;; FIXME
  "If nil, that's not an error, that just means we don't filter!"
  [input-map]
  (let [strs (get-day-hour-minute input-map)]
    (if (not (and (= 3 (count strs))
                  (not-any? clojure.string/blank? strs)))
      nil
      ;; TODO:
      ;; try / catch, in case (Integer.) doesn't work.
      ;; validate:
      ;;   wday:   [0..6]
      ;;   hour:   [0..23]
      ;;   minute: [0..59]
      {:wday   (Integer. (get strs 0))
       :hour   (Integer. (get strs 1))
       :minute (Integer. (get strs 2))
       })))
