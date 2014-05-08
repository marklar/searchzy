(ns searchzy.service.bmis.price
  "Price metadata for BizMenuItems"
  (:require [searchzy.service
             [util :as util]]))

(defn- val-and-biz
  ":: (keyword, [bmi]) -> {price, business}"
  [attr bmi]
  {:value (get bmi attr)
   :business (:business bmi)})

(defn- min-price
  ":: [bmi] -> {price, business}"
  [sorted-bmis]
  (let [bmi (first sorted-bmis)]
    (val-and-biz :price_micros bmi)))

(defn- max-price
  ":: [bmi] -> {price, business}"
  [sorted-bmis]
  (let [bmi (last sorted-bmis)]
    (val-and-biz :price_micros bmi)))

(defn- median-price
  ":: [bmi] -> {price, business}"
  [sorted-bmis]
  (let [cnt (count sorted-bmis)
        idx (quot cnt 2)
        bmi (nth sorted-bmis idx)]
    (val-and-biz :price_micros bmi)))
 
(defn- mean-price
  ":: [bmi] -> float"
  [sorted-bmis]
  (let [prices (util/compact (map :price_micros sorted-bmis))
        sum (apply + prices)
        cnt (count prices)]
    (/ sum cnt)))

(defn metadata
  [bmis]
  (let [priced-bmis (filter :price_micros bmis)
        sorted-bmis (sort-by :price_micros priced-bmis)]
    (if (empty? sorted-bmis)
      ;; none
      {}
      ;; some...
      {:min    (min-price sorted-bmis)
       :max    (max-price sorted-bmis)
       :median (median-price sorted-bmis)
       :mean   (mean-price sorted-bmis)})))
