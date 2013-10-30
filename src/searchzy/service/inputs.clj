(ns searchzy.service.inputs
  (:require [clojure.string :as str]))

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

