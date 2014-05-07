(ns searchzy.test.service.geo-util
  (:use midje.sweet)
  (:require [searchzy.service.geo-util :as g]))

(fact "`geo-str->map`"
      (let [f #'g/geo-str->map]
        (f "30.4")        => (throws Exception)
        (f "30.4,")       => (throws Exception)
        (f "30.4,-122.9") => {:lat 30.4, :lon -122.9}
        ))

;; No unit tests for `haversine` or `haversine-from-strs`.
