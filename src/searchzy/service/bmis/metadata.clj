(ns searchzy.service.bmis.metadata
  "For BizMenuItems -- >>>>>>> VERSION 1 <<<<<<<"
  (:require [searchzy.service
             [util :as util]]))

(defn- get-prices-micros
  [biz-menu-items]
  (let [prices (util/compact (map :price_micros biz-menu-items))
        sum (apply + prices)
        cnt (count prices)]
    (if (= 0 cnt)
      {:mean 0, :max 0, :min 0}
      {:mean (/ sum cnt)
       :max  (apply max prices)
       :min  (apply min prices)})))

(defn- get-all-hours-today
  [biz-menu-items day-of-week]
  (let [all-hours (util/compact (map #(-> % :business :hours) biz-menu-items))]
    (util/compact (map #(util/get-hours-for-day % day-of-week) all-hours))))

(def HOUR_MINS 60)

(defn- mins-to-hour
  [minutes]
  (let [m (mod minutes HOUR_MINS)]
    {:hour   (/ (- minutes m) HOUR_MINS)
     :minute m}))

(defn- get-latest-hour
  "Given [{:hour h :minute m}], return the latest one."
  [hour-list]
  (let [max-minutes (apply max (util/compact (map util/time->mins hour-list)))]
    (mins-to-hour max-minutes)))

(defn- get-latest-close
  "Given biz-menu-items, return a single 'hour' (i.e. {:hour h, :minute m})."
  [biz-menu-items day-of-week]
  (let [all-closing (map :close (get-all-hours-today biz-menu-items day-of-week))]
    (get-latest-hour all-closing)))

;;----------------------

(defn get-metadata
  [biz-menu-items day-of-week]
  {:prices-micros (get-prices-micros biz-menu-items)
   :latest-close (get-latest-close biz-menu-items day-of-week)})
