(ns searchzy.service.geo-sort
  "Code taken from Elastisch and modified to allow a GeoDistanceSortBuilder."
  (:import [org.elasticsearch.search.sort GeoDistanceSortBuilder SortOrder]
           [org.elasticsearch.common.unit DistanceUnit]))

(defn mk-geo-distance-sort-builder
  "Create a GeoDistanceSortBuilder"
  [coords order]
  (if (nil? coords)
    ;; If no coords, then probably we're using a polygon geofilter,
    ;; and sorting by distance doesn't make sense.
    ;; So we just return a nil SortBuilder.
    nil
    ;; Okay, let's actually sort by distance...
    (let [o  (if (= order :asc) SortOrder/ASC SortOrder/DESC)
          sb (GeoDistanceSortBuilder. "latitude_longitude")] 
      (doto sb
        (.point (:lat coords) (:lon coords))
        (.order o)
        (.unit DistanceUnit/MILES))
    sb)))
