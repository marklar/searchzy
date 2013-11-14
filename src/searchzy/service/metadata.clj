(ns searchzy.service.metadata   ;; for businesses
  (:require [searchzy.service
             [util :as util]]))

(defn- compact [seq] (remove nil? seq))

(defn- get-prices-micros
  [{hits :hits}]
  (let [prices (compact (map #(-> % :_source :price_micros) hits))
        sum (apply + prices)
        cnt (count prices)]
    (if (= 0 cnt)
      {:mean 0, :max 0, :min 0}
      {:mean (/ sum cnt)
       :max  (apply max prices)
       :min  (apply min prices)})))

(defn- get-all-hours-today
  [hits]
  (let [day-of-week (util/get-day-of-week)
        all-hours (compact (map #(-> % :_source :business :hours) hits))]
    (compact (map #(util/get-hours-today % day-of-week) all-hours))))

(def HOUR_MINS 60)

(defn- mins-to-hour
  [minutes]
  (let [m (mod minutes HOUR_MINS)]
    {:hour   (/ (- minutes m) HOUR_MINS)
     :minute m}))

(defn- get-latest-hour
  "Given [{:hour h :minute m}], return the latest one."
  [hour-list]
  (let [as-minutes (fn [{h :hour, m :minute}] (+ m (* HOUR_MINS h)))
        max-minutes (apply max (cons 0 (map as-minutes hour-list)))]
    (mins-to-hour max-minutes)))

(defn- get-latest-close
  "Given ES hits, return a single 'hour' (i.e. {:hour h, :minute m})."
  [{hits :hits}]
  (let [all-closing (map :close (get-all-hours-today hits))]
    (get-latest-hour all-closing)))

(defn get-metadata
  [big-item-res]
  {:prices-micros (get-prices-micros big-item-res)
   :latest-close (get-latest-close big-item-res)})
