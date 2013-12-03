(ns searchzy.service.flurbl
  "Code taken from Elastisch and modified to allow a GeoDistanceSortBuilder."
  (:import [org.elasticsearch.search.sort GeoDistanceSortBuilder SortOrder]
           [org.elasticsearch.common.unit DistanceUnit]))

(defn mk-geo-distance-sort-builder
  "Create a GeoDistanceSortBuilder"
  [coords order]
  (let [o  (if (= order :asc) SortOrder/ASC SortOrder/DESC)
        sb (GeoDistanceSortBuilder. "latitude_longitude")] 
    (doto sb
      (.point (:lat coords) (:lon coords))
      (.order o)
      (.unit DistanceUnit/MILES))
    sb))
