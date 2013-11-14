(ns searchzy.service.util
  (:import [java.util Calendar GregorianCalendar]))

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

(defn- geo-str-to-map
  "geo-point string to geo-point map"
  [s]
  (let [[lat-str lon-str] (clojure.string/split s #",")]
    {:lat (read-string lat-str)
     :lon (read-string lon-str)}))

(defn haversine-from-strs
  "Find geo distance between to geo-points."
  [loc1-str loc2-str]
  (let [loc1 (geo-str-to-map loc1-str)
        loc2 (geo-str-to-map loc2-str)]
    (haversine loc1 loc2)))

;; -----

(defn mk-suggestion-query
  "String 's' may contain mutiple terms.
   Perform a boolean query."
  [s]
  ;; split s into tokens.
  ;; take all but last token: create :term query for each.
  ;; take last token: create prefix match.
  (let [tokens (clojure.string/split s #" ")
        prefix (last tokens)
        fulls  (butlast tokens)]
    (let [foo (cons {:prefix {:name prefix}}
                    (map (fn [t] {:term {:name t}}) fulls))]
      {:bool {:must foo}})))

;; -- hours --

(defn- day-hour-maps-to-alist
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
  (let [alist       (day-hour-maps-to-alist hours)
        num-2-hours (apply hash-map (flatten alist))]
    (get num-2-hours day-of-week)))

(defn get-day-of-week
  "Return an int: [0..6]."
  []
  (let [gc (GregorianCalendar.)]
    ;; In Java, days are numbered 1-7, so decrement.
    (dec (.get gc Calendar/DAY_OF_WEEK))))

(defn- time-cmp-eq
  [op
   {h1 :hour, m1 :minute}
   {h2 :hour, m2 :minute}]
  (or (op h1 h2)
      (and (= h1 h2)
           (or (= m1 m2) (op m1 m2)))))


;; FIXME: make more generic!
(defn- open-at?
  "Time: 10:51.  Open: 8:30.  Close: 15:00.  --> true"
  [hours-map biz-menu-item-result]
  (let [biz-hours (:hours (:business (:_source biz-menu-item-result)))
        hours-today (get-hours-today biz-hours (:wday hours-map))]
    (if (not (and hours-today (:open hours-today) (:close hours-today)))
      false
      (and (time-cmp-eq > hours-map (:open hours-today))
           (time-cmp-eq < hours-map (:close hours-today))))))

(defn filter-by-hours
  [results hours-map]
  (let [new-hits (filter #(open-at? hours-map %) (:hits results))]
    (assoc results
      :hits new-hits
      :total (count new-hits))))
