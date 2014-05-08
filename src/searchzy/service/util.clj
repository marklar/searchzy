(ns searchzy.service.util
  (:import [java.util Calendar GregorianCalendar TimeZone])
  (:require [searchzy.service
             [responses :as responses]
             [tz :as tz]]))

;; Assert that the input values are good.
(defn mk-geo-filter
  "Create map for filtering by geographic distance."
  [{:keys [miles coords]}]
  (let [coords-str (str (:lat coords) "," (:lon coords))]
    {:geo_distance {:distance (str miles "miles")
                    :latitude_longitude coords-str}}))

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

;; private?
(defn get-hours-for-day
  "Specifically related to how hours are stored in a Business object.
   Probably doesn't really belong in util."
  [week-of-hours day-of-week]
  ;; We cannot rely on the hours being a complete list.
  ;; Sometimes, there'll be a day or more missing.
  ;; It's necessary to look at each entry's :wday.
  (let [alist       (day-hour-maps-to-alist week-of-hours)
        num-2-hours (apply hash-map (flatten alist))]
    (let [res (get num-2-hours day-of-week)]
      (if (nil? res)
        {:open {}, :close {}}
        res))))

(defn- two-digit-str
  "0  => '00'
   15 => '15'
  "
  [non-neg-int]
  (let [s (str non-neg-int)]
    (if (< (count s) 2)
      (str "0" s)
      s)))

(defn- mk-tz-str
  [utc-offset-map]
  (if (empty? utc-offset-map)
    nil
    (let [{:keys [hours minutes]} utc-offset-map]
      (str "GMT"
           (if (< hours 0) "-" "+")
           (two-digit-str (Math/abs hours)) ":"
           (two-digit-str minutes)))))

;; New York City
(def DEFAULT-OFFSET {:hours -5, :minutes 0})

(defn- mk-tz
  [rails-time-zone utc-offset-map]
  (TimeZone/getTimeZone (or (get tz/rails-tz-2-tz-info rails-time-zone)
                            (mk-tz-str utc-offset-map)
                            (mk-tz-str DEFAULT-OFFSET))))

;; No unit test.  Side effects (GregorianCalendar).
(defn get-day-of-week-from-tz
  "Return an int: [0..6].  0 = Sunday.
   e.g. args:
     rails-time-zone: 'Eastern Time (US & Canada)'
     utc-offset-map:  {:hours -5, :minutes 0}
  "
  [rails-time-zone utc-offset-map]
  (let [tz      (mk-tz rails-time-zone utc-offset-map)
        cal     (doto (GregorianCalendar.) (.setTimeZone tz))
        cal-num (.get cal Calendar/DAY_OF_WEEK)]
    ;; In Calendar class, days are numbered 1-7, so decrement.
    (dec cal-num)))

(defn get-day-of-week
  "Return an int: [0..6].  0 = Sunday."
  [hours-map rails-time-zone utc-offset-map]
  (if (:wday hours-map)
    (:wday hours-map)
    (get-day-of-week-from-tz rails-time-zone utc-offset-map)))

;; public
(defn time->mins
  [{h :hour, m :minute}]
  (try
    (+ m (* h 60))
    (catch Exception e 0)))

(defn- time-cmp
  [op t1 t2]
  (op (time->mins t1) (time->mins t2)))

(defn- valid-hours?
  [hours]
  (let [o (:open hours)
        c (:close hours)]
    (and hours o c
       (:hour o) (:minute o)
       (:hour c) (:minute c))))

;;------------------

(defn open-at?
  "Open: 8:30.  Close: 15:00.
     Time: 10:51 => true
     Time: 15:00 => false"
  [hours-map week-of-biz-hours]
  (let [hours-today (get-hours-for-day week-of-biz-hours (:wday hours-map))]
    (if (not (valid-hours? hours-today))
      false
      (and (time-cmp >= hours-map (:open  hours-today))
           (time-cmp <  hours-map (:close hours-today))))))

;; 1.  clean input
;; 2a. return error-json of errs if present, else
;; 2b. perform search with valid-args
(defn validate-and-search
  [input-args clean-fn search-fn]
  (let [[valid-args errs] (clean-fn input-args)]
    (if (seq errs)
      (responses/error-json {:errors errs})
      (search-fn valid-args))))
