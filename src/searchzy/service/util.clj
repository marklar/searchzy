(ns searchzy.service.util
  (:import [java.util Calendar GregorianCalendar]))

(defn -day-hour-maps-to-alist
  [hour-maps]
  (map (fn [{:keys [wday hours]}] [wday hours])
       hour-maps))

(defn get-hours-today
  "Specifically related to how hours are stored in a Business object.
   Probably doesn't really belong in util."
  [hours day-of-week]
  ;; We cannot rely on the hours being a complete list.
  ;; Sometimes, there'll be a day or more missing.
  ;; It's necessary to look at each entry's :wday.
  (let [alist       (-day-hour-maps-to-alist hours)
        num-2-hours (apply hash-map (flatten alist))]
    (num-2-hours day-of-week)))

(defn get-day-of-week
  []
  (let [gc (GregorianCalendar.)]
    (.get gc Calendar/DAY_OF_WEEK)))

;; -- GEO-RELATED -- could be moved into searchzy.service.geo.

(defn mk-geo-filter
  "Create map for filtering by geographic distance."
  [{:keys [miles coords]}]
  (let [coords-str (str (:lat coords) "," (:lon coords))]
    {:geo_distance {:distance (str miles "mi")
                    :latitude_longitude coords-str}}))

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
