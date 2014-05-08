(ns searchzy.service.bmis.hours
  "For BizMenuItems"
  (:require [searchzy.service
             [util :as util]]))

(defn- bmi->hours-today
  [bmi day-of-week]
  (let [hours (-> bmi :business :hours)]
    (util/get-hours-for-day hours day-of-week)))

(defn- bmi->with-mins
  ":: (BMI, int) -> [BMI, int, int]"
  [bmi day-of-week]
  (let [hours (bmi->hours-today bmi day-of-week)]
    (if-not hours
      nil
      [bmi, (util/time->mins (:open hours)), (util/time->mins (:close hours))])))

;;
;; TODO: What about never closes???
;;
(defn- alist-bmi-open-close-mins
  [bmis day-of-week]
  (util/compact
   (map #(bmi->with-mins % day-of-week) bmis)))

(defn- earlier
  [vec1 vec2]
  (if (<= (nth vec1 1) (nth vec2 1))
    vec1
    vec2))

(defn- get-earliest-open
  [alist day-of-week]
  (let [good-vecs (filter #(nth % 1) alist)
        earliest-vec (reduce earlier good-vecs)
        bmi (first earliest-vec)]
    {:value (:open (bmi->hours-today bmi day-of-week))
     :business (:business bmi)}))

(defn- later
  [vec1 vec2]
  (if (>= (nth vec1 2) (nth vec2 2))
    vec1
    vec2))

(defn- get-latest-close
  [alist day-of-week]
  (let [good-vecs (filter #(nth % 2) alist)
        latest-vec (reduce later good-vecs)
        bmi (first latest-vec)]
    {:value (:close (bmi->hours-today bmi day-of-week))
     :business (:business bmi)}))

(defn metadata
  [bmis day-of-week]
  (let [alist (alist-bmi-open-close-mins bmis day-of-week)]
    {:earliest (get-earliest-open alist day-of-week)
     :latest   (get-latest-close alist day-of-week)}))
