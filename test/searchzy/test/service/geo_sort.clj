(ns searchzy.test.service.geo-sort
  (:use midje.sweet)
  (:import [org.elasticsearch.search.sort GeoDistanceSortBuilder])
  (:require [searchzy.service.geo-sort :as g]))

(fact "`mk-geo-distance-sort-builder`"
      (let [f g/mk-geo-distance-sort-builder]
        (instance? GeoDistanceSortBuilder
                   (f {:lat 30, :lon -122} :asc))
        => TRUTHY
        ))

