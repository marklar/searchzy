(ns searchzy.service.bmis.filter
  (:use [clojure.core.match :only (match)]
        [clojure.contrib.seq-utils :only [separate]])
  (:require [searchzy.service.bmis
             [value :as value]]
            [searchzy.service
             [geo-util :as geo-util]
             [util :as util]]))

(defn- get-dist
  [bmi]
  (get-in bmi [:business :distance_in_mi]))

;;------------------------------------
;; collaring - not currently in use?
;;------------------------------------

(defn- item-idx-alist [xs]
  (map vector xs (iterate inc 0)))

(defn- maybe-collar
  "Take only the closest results, stopping when you:
      - have enough AND you're at least 1m out  -OR-
      - run out."
  [collar-map include-unpriced unsorted-bmis]
  (let [
        ;; If include-unpriced, must re-sort by distance before collaring!
        ;; asc distance_in_mi
        ;; TODO: rather than SORTING, we really ought to 'zipper' together
        ;; the priced-bmis and the unpriced-bizs.
        bmis (if include-unpriced
               (sort-by get-dist unsorted-bmis)
               unsorted-bmis)
        mr (:min-results collar-map)
        miles 1.0]
    (if (nil? mr)
      bmis
      (let [need-more? (fn [[item idx]]
                         (or (< idx mr)
                             (< (get-dist item) miles)))
            ]
        (map first (take-while need-more? (item-idx-alist bmis)))))))

;;----------------------------------
  
(defn- sort-by-price
  "1. asc/desc price (for those with price),
   2. asc distance_in_mi"
  [sort-map bmis]
  (let [[priced unpriced] (separate :price_micros bmis)
        asc-priced        (sort-by (juxt :price_micros get-dist) priced)
        asc-unpriced      (sort-by get-dist unpriced)
        maybe-rev         (if (= :desc (:order sort-map)) reverse identity)]
    (concat (maybe-rev asc-priced)
            (maybe-rev asc-unpriced))))

(defn- maybe-sort
  [include-unpriced sort-map bmis]
  (let [desc? (= :desc (:order sort-map))]
    (match (:attribute sort-map)
           
           "distance" (let [xs (if include-unpriced
                                 (sort-by get-dist bmis)
                                 bmis)]
                        (if desc? (reverse xs) xs))
           
           "price"    (sort-by-price sort-map bmis)
           
           "rating"   (let [xs (value/rate-and-sort bmis)]
                        (if desc? (reverse xs) xs))
           
           "value"    (let [xs (value/value-and-sort bmis)]
                        (if desc? (reverse xs) xs)))))

;;----------------------

;; FIXME: What if there are no coords?  Returns nil?
(defn- add-distance-in-mi
  [bmi coords]
  (if-let [bmi-coords (get-in bmi [:business :coordinates])]
    (let [distance (geo-util/haversine bmi-coords coords)]
      (assoc-in bmi [:business :distance_in_mi] distance))))

;; use 'partial'
(defn- add-distances
  [coords bmis]
  (map #(add-distance-in-mi % coords) bmis))

(defn- maybe-filter-by-hours
  [hours-map bmis]
  (if (= {} hours-map)
    bmis
    (filter #(util/open-at? hours-map (get-in % [:business :hours]))
            bmis)))

;;
;; TODO: Why in the world are these "empties" even there?
;;
(defn- has-id? [bmi]
  (get-in bmi [:business :_id]))

;;--------------------------

;; FIXME: Doesn't use `maybe-collar`?
(defn filter-sort
  [bmis include-unpriced geo-map hours-map sort-map]
  (->> bmis
       (filter has-id?)
       (maybe-filter-by-hours hours-map)
       (add-distances (:coords geo-map))
       (maybe-sort include-unpriced sort-map)))
