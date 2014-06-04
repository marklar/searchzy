(ns searchzy.service.bmis.filter
  (:use [clojure.core.match :only (match)]
        [clojure.contrib.seq-utils :only [separate]])
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
  "The bmis enter here in ASC order.
   If that's what's requested, simply return them.
   If DESC is requested, reverse them.
  "
  [sort-map bmis]
  ;; >> sort-by-price already reverse-sorted if necessary
  (if (or (= "price" (:attribute sort-map))
          (= :asc (:order sort-map)))
    bmis
    (reverse bmis)))

;; PREVIOUSLY, WE INTENDED TO SORT BY DISTANCE, TOO.
;; WOULD HAVE TO INCLUDE IT IN ns:value AND WEIGHT.
(defn- foobar
  [bmi]
  [(:awesomeness bmi)
   (- 0 (-> bmi :business :distance_in_mi))])

(defn- sort-by-price
  "1. asc/desc price (for those with price),
   2. asc distance_in_mi"
  [sort-map bmis]
  (let [[priced unpriced] (separate :price_micros bmis)
        asc-priced (sort-by (fn [i] [(:price_micros i)
                                     (-> i :business :distance_in_mi)]) priced)
        asc-unpriced (sort-by (fn [i] (-> i :business :distance_in_mi)) unpriced)
        maybe-rev (if (= :asc (:order sort-map)) identity reverse)]
    (concat (maybe-rev asc-priced) (maybe-rev asc-unpriced))))

(defn- maybe-sort
  [sort-map bmis]
  (match (:attribute sort-map)
         ;; asc distance_in_mi (already done in maybe-collar)
         "distance" bmis
         ;; 1. asc/desc price (for those with price), 2. asc distance_in_mi
         "price"    (sort-by-price sort-map bmis)
         ;; asc rating (which is rarely what we'll want)
         "rating"   (value/rate-and-sort bmis)
         ;; asc value (which is rarely what we'll want)
         "value"    (value/value-and-sort bmis)))

(defn- maybe-re-sort
  [sort-map bmis]
  (->> bmis
       (maybe-sort sort-map)
       (maybe-reverse sort-map)))

(defn- item-idx-alist
  [xs]
  (map vector
       xs
       (iterate inc 0)))

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
               (sort-by #(-> % :business :distance_in_mi) unsorted-bmis)
               unsorted-bmis)
        mr (:min-results collar-map)
        miles 1.0]
    (if (nil? mr)
      bmis
      (let [get-dist    #(-> % :business :distance_in_mi)
            need-more? (fn [[item idx]]
                         (or (< idx mr)
                             (< (get-dist item) miles)))
            ]
        (map first (take-while need-more? (item-idx-alist bmis)))))))

(defn- add-distance-in-mi
  [bmi coords]
  (let [bmi-coords (-> bmi :business :coordinates)]
    (if bmi-coords
      (let [dist (geo-util/haversine bmi-coords coords)]
        (assoc-in bmi [:business :distance_in_mi] dist)))))

;; use 'partial'
(defn- add-distances
  [geo-map bmis]
  (map #(add-distance-in-mi % (:coords geo-map)) bmis))

(defn- maybe-sort-by-distance-again
  [include-unpriced sort-map bmis]
  (if (and include-unpriced (= "distance" (:attribute sort-map)))
    (sort-by #(-> % :business :distance_in_mi) bmis)
    bmis))

;;
;; TODO: Why in the world are these "empties" even there?
;;
(defn- reject-empties
  [bmis]
  (filter #(-> % :business :_id) bmis))

;;--------------------------

(defn filter-sort
  [bmis include-unpriced geo-map hours-map sort-map]
  (->> bmis
       reject-empties
       (maybe-filter-by-hours hours-map)
       (add-distances geo-map)
       (maybe-sort-by-distance-again include-unpriced sort-map)
       (maybe-re-sort sort-map)))
