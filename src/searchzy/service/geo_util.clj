(ns searchzy.service.geo-util
  "GEO-RELATED"
  )

(defn- coords->str
  [coords]
  (str (:lat coords) "," (:lon coords)))

(defn- mk-circle-geo-filter
  "Create map for filtering by geographic distance."
  [coords miles]
  (let [coords-str (coords->str coords)]
    {:geo_distance {:distance (str miles "miles")
                    :latitude_longitude coords-str}}))

;; http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-polygon-filter.html
(defn- mk-polygon-geo-filter
  [points]
  {:geo_polygon {:latitude_longitude {:points (map coords->str points)}}})

;; Assert that the input values are good.
(defn mk-geo-filter
  "Create map for filtering by geographic distance."
  [{:keys [polygon coords miles]}]
  (if-not (nil? polygon)
    (mk-polygon-geo-filter polygon)
    (mk-circle-geo-filter coords miles)))

;;---------------------------------

(defn- geo-str->map
  "geo-point string to geo-point map"
  [s]
  (let [[lat-str lon-str] (clojure.string/split s #",")]
    {:lat (read-string lat-str)
     :lon (read-string lon-str)}))

;;----------------------------------
;; polygons

(defn polygon-str->coords
  [polygon-str]
  (if (empty? polygon-str)
    nil
    (map geo-str->map
         (clojure.string/split polygon-str #";"))))

(defn valid-polygon?
  [coords]
  (if (nil? coords)
    false
    (< 2 (count coords))))

;;----------------------------------
;; Haversine

(defn haversine
  "Find geo distance between to geo-points.
   http://rosettacode.org/wiki/Haversine_formula#Clojure"
  [{lat1 :lat lon1 :lon} {lat2 :lat lon2 :lon}]
  (if (or (nil? lat1) (nil? lon1) (nil? lat2) (nil? lon2))
    nil
    (let [R    3959.87 ; miles
          dlat (Math/toRadians (- lat2 lat1))
          dlon (Math/toRadians (- lon2 lon1))
          lat1 (Math/toRadians lat1)
          lat2 (Math/toRadians lat2)
          a    (+ (* (Math/sin (/ dlat 2)) (Math/sin (/ dlat 2)))
                  (* (Math/sin (/ dlon 2)) (Math/sin (/ dlon 2))
                     (Math/cos lat1) (Math/cos lat2)))]
      (* R 2 (Math/asin (Math/sqrt a))))))

(defn haversine-from-strs
  "Find geo distance between to geo-points."
  [loc1-str loc2-str]
  (let [loc1 (geo-str->map loc1-str)
        loc2 (geo-str->map loc2-str)]
    (haversine loc1 loc2)))
