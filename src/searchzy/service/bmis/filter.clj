(ns searchzy.service.bmis.filter
  (:use [clojure.core.match :only (match)])
  (:require [searchzy.service.bmis
             [value :as value]]
            [searchzy.service
             [geo-util :as geo-util]
             [util :as util]]))
  

(defn- maybe-filter-by-hours
  [hours-map bmis]
  (if (= {} hours-map)
    bmis
    (filter #(util/open-at? hours-map (-> % :business :hours))
            bmis)))

(defn- maybe-reverse
  [sort-map bmis]
  (if (= :desc (:order sort-map))
    bmis
    (reverse bmis)))

(defn- maybe-re-sort
  [sort-map bmis]
  (match (:attribute sort-map)
         ;; "distance" bmis   ;; Already handles 'reverse' within ES.
         "distance" (->> bmis
                         (sort-by #(-> % :business :distance_in_mi))
                         (maybe-reverse sort-map))
         "price"    (->> bmis
                         (sort-by (fn [i] [(:price_micros i)
                                           (- 0 (-> i :business :distance_in_mi)) ]))
                         (maybe-reverse sort-map))
         "value"    (->> bmis
                         value/score-and-sort
                         (maybe-reverse sort-map))))

(defn- maybe-collar
  "Take only the closest results, stopping when you:
      - have enough AND you're at least 1m out  -OR-
      - run out."
  [collar-map bmis]
  (let [mr (:min-results collar-map)
        miles 1.0]
    (if (nil? mr)
      bmis
      (let [get-dist    #(-> % :business :distance_in_mi)
            need-more? (fn [[item idx]]
                         (or (< idx mr)
                             (< (get-dist item) miles)))
            ]
        (map first
             (take-while need-more?
                         (map vector
                              bmis
                              (iterate inc 0))))))))

(defn- add-distance-in-mi
  [item coords]
  (let [item-coords (-> item :business :coordinates)
        dist        (geo-util/haversine item-coords coords)]
    (assoc-in item [:business :distance_in_mi] dist)))

;; use 'partial'
(defn- add-distances
  [geo-map bmis]
  (map #(add-distance-in-mi % (:coords geo-map)) bmis))

;;--------------------------

(defn filter-collar-sort
  [bmis geo-map collar-map hours-map sort-map]
  (->> bmis
       (add-distances geo-map)
       (maybe-filter-by-hours hours-map)
       (maybe-collar collar-map)
       (maybe-re-sort sort-map)))
