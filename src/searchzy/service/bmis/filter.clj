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
  "The bmis enter here in ASC order.
   If that's what's requested, simply return them.
   If DESC is requested, reverse them."
  [sort-map bmis]
  (if (= :asc (:order sort-map))
    bmis
    (reverse bmis)))

(defn- maybe-re-sort
  [sort-map bmis]
  (match (:attribute sort-map)
         "distance" (->> bmis
                         ;; asc distance_in_mi (already done in maybe-collar)
                         (maybe-reverse sort-map))
         "price"    (->> bmis
                         ;; 1. asc price, 2. asc distance_in_mi
                         (sort-by (fn [i] [(or (:price_micros i) Integer/MAX_VALUE)
                                           (-> i :business :distance_in_mi) ]))
                         (maybe-reverse sort-map))
         "value"    (->> bmis
                         ;; asc value (which is rarely what we'll want)
                         value/score-and-sort
                         ;; 1. asc value, 2. desc distance_in_mi
                         (sort-by (fn [i] [(:awesomeness i)
                                           (- 0 (-> i :business :distance_in_mi)) ]))
                         (maybe-reverse sort-map))))

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
  [bmis include-unpriced geo-map collar-map hours-map sort-map]
  (->> bmis
       (add-distances geo-map)
       (maybe-filter-by-hours hours-map)
       (maybe-collar collar-map include-unpriced)
       (maybe-re-sort sort-map)))
