(ns searchzy.service.inputs
  (:require [clojure.string :as str]
            [searchzy.service [geo :as geo]]))

(defn str-to-val
  "Taking an HTTP query parameter and convert it to a proper value,
   using a default value if none provided."
  [str default]
  (if (str/blank? str)
    default
    (let [val (read-string str)]
      (if (nil? val)
        default
        val))))

(defn true-str?
  "Convert string (e.g. from cmd-line or http params) to bool."
  [s]
  (contains? #{"true" "t" "1"} s))

(defn mk-geo-map
  "Take input-geo-map: miles, address, lat, lon.
   If the input is valid, create a geo-map.
   If not, return nil."
  [{address :address, lat-str :lat, lon-str :lon, miles-str :miles}]
  (let [lat   (str-to-val lat-str   nil)
        lon   (str-to-val lon-str   nil)
        miles (str-to-val miles-str 4.0)]
    (let [coords (geo/get-lat-lon lat lon address)]
      (if coords
        {:address address, :coords coords, :miles miles}
        nil))))

(defn mk-page-map
  "From input info, create usable info."
  [{:keys [from size]}]
  {:from (str-to-val from 0)
   :size (str-to-val size 10)})
