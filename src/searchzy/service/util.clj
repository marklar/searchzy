(ns searchzy.service.util
  (:import [java.util Calendar GregorianCalendar TimeZone])
  (:require [searchzy.service [tz :as tz]]))

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

(defn get-hours-for-day
  "Specifically related to how hours are stored in a Business object.
   Probably doesn't really belong in util."
  [hours day-of-week]
  ;; We cannot rely on the hours being a complete list.
  ;; Sometimes, there'll be a day or more missing.
  ;; It's necessary to look at each entry's :wday.
  (let [alist       (day-hour-maps-to-alist hours)
        num-2-hours (apply hash-map (flatten alist))]
    (get num-2-hours day-of-week)))

(defn- two-digit-str
  "0 =>  '00'
   15 => '15' "
  [int]
  (let [s (str int)]
    (if (< (count s) 2)
      (str "0" s)
      s)))

(defn- mk-tz-str
  [utc-offset-map]
  (if (nil? utc-offset-map)
    nil
    (let [{:keys [hours minutes]} utc-offset-map]
      (str "GMT" (if (< hours 0) "" "+")
           (two-digit-str hours) ":"
           (two-digit-str minutes)))))

(defn- mk-tz
  [rails-time-zone utc-offset-map]
  (TimeZone/getTimeZone (or (get tz/rails-tz-2-tz-info rails-time-zone)
                            (mk-tz-str utc-offset-map)
                            ;; use default time zone
                            (mk-tz-str {:hours -5, :minutes 0}))))

(defn get-day-of-week
  "Return an int: [0..6]."
  [hours-map rails-time-zone utc-offset-map]
  (if (:wday hours-map)
    (:wday hours-map)
    (let [tz      (mk-tz rails-time-zone utc-offset-map)
          cal     (doto (GregorianCalendar.) (.setTimeZone tz))
          cal-num (.get cal Calendar/DAY_OF_WEEK)]
        ;; In Calendar class, days are numbered 1-7, so decrement.
        (dec cal-num))))

(defn time-to-mins
  [{h :hour, m :minute}]
  (+ (* h 60) m))

(defn time-cmp
  [op t1 t2]
  (op (time-to-mins t1) (time-to-mins t2)))

(defn valid-hours?
  [hours]
  (let [o (:open hours)
        c (:close hours)]
    (and hours o c
       (:hour o) (:minute o)
       (:hour c) (:minute c))))

(defn open-at?
  "Open: 8:30.  Close: 15:00.
     Time: 10:51 => true
     Time: 15:00 => false"
  [hours-map biz-hours]
  (let [hours-today (get-hours-for-day biz-hours (:wday hours-map))]
    (if (not (valid-hours? hours-today))
      false
      (and (time-cmp >= hours-map (:open hours-today))
           (time-cmp <  hours-map (:close hours-today))))))
