(ns searchzy.service.util
  (:import [java.util Calendar GregorianCalendar]))

(defn get-day-of-week
  []
  (let [gc (GregorianCalendar.)]
    (.get gc Calendar/DAY_OF_WEEK)))

;; These are all geo-related -- could be moved into searchzy.service.geo.

(defn mk-geo-filter
  "Create map for filtering by geographic distance."
  [miles lat lon]
  {:geo_distance {:distance (str miles "mi")
                  :latitude_longitude (str lat "," lon)}})

;; Haversine

(defn haversine
  "Find geo distance between to geo-points.
   http://rosettacode.org/wiki/Haversine_formula#Clojure"
  [{lat1 :lat lon1 :lon} {lat2 :lat lon2 :lon}]
  (let [R    3959.87 ; miles
        dlat (Math/toRadians (- lat2 lat1))
        dlon (Math/toRadians (- lon2 lon1))
        lat1 (Math/toRadians lat1)
        lat2 (Math/toRadians lat2)
        a    (+ (* (Math/sin (/ dlat 2)) (Math/sin (/ dlat 2)))
                (* (Math/sin (/ dlon 2)) (Math/sin (/ dlon 2))
                   (Math/cos lat1) (Math/cos lat2)))]
    (* R 2 (Math/asin (Math/sqrt a)))))

(defn -geo-str-to-map
  "geo-point string to geo-point map"
  [s]
  (let [[lat-str lon-str] (clojure.string/split s #",")]
    {:lat (read-string lat-str)
     :lon (read-string lon-str)}))

(defn haversine-from-strs
  "Find geo distance between to geo-points."
  [loc1-str loc2-str]
  (let [loc1 (-geo-str-to-map loc1-str)
        loc2 (-geo-str-to-map loc2-str)]
    (haversine loc1 loc2)))
