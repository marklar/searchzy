(ns searchzy.test.service.util
  (:use midje.sweet)
  (:require [searchzy.service.util :as util]))

(fact "`mk-geo-filter`"
      (let [f util/mk-geo-filter]
        (f {:miles 3
            :coords {:lat 30, :lon -120}})
        => {:geo_distance {:distance "3miles"
                           :latitude_longitude "30,-120"}}

        (f {:miles 4.5
            :coords {:lat 30, :lon -120}})
        => {:geo_distance {:distance "4.5miles"
                           :latitude_longitude "30,-120"}}
        ))

(fact "`mk-suggestion-query`"
      (let [f util/mk-suggestion-query]
        ;; just one token
        (f "one") => {:bool {:must [{:prefix {:name "one"}}]}}
        ;; multiple tokens
        (f "one two thr") => {:bool {:must [{:prefix {:name "thr"}}
                                            {:term {:name "one"}}
                                            {:term {:name "two"}}
                                            ]}}
        ))

(let [hours-0 {:open {:hour 12, :minute 0}
               :close {:hour 6, :minute 0}}
      hours-1 {:open {:hour 10, :minute 0}
               :close {:hour 20, :minute 0}}
      hours-2 {:open {:hour 10, :minute 0}
               :close {:hour 18, :minute 0}}

      week-of-hours [{:wday 0, :hours hours-0}
                     {:wday 1, :hours hours-1}
                     {:wday 2, :hours hours-2}]
      ]


  (fact "`day-hour-maps-to-alist`"
        (let [f #'util/day-hour-maps-to-alist]
          (f week-of-hours) => [[0 hours-0] [1 hours-1] [2 hours-2]]
          ))

  (fact "`get-hours-for-day`"
        (let [f util/get-hours-for-day]
          (f week-of-hours 2) => hours-2
          ))
)

(fact "`two-digit-str`"
      (let [f #'util/two-digit-str]
        (f "0")  => "00"
        (f "15") => "15"
        ))

(fact "`mk-tz-str`"
      (let [f #'util/mk-tz-str]
        (f {}) => nil
        (f {:hours  0, :minutes 0})  => "GMT+00:00"
        (f {:hours -5, :minutes 45}) => "GMT-05:45"
        ))

;; No unit test for:
;;   `mk-tz`
;;   `get-day-of-week-from-tz`
;;   `get-day-of-week`
;;   `validate-and-search`

(fact "`time->mins`"
      (let [f util/time->mins]
        ;; weird times
        (f nil) => nil
        (f {})  => nil
        (f {:hour 10}) => nil
        ;; good times
        (f {:hour 0,  :minute 30}) => 30
        (f {:hour 10, :minute 41}) => 641
        ))

(fact "`time-cmp`"
      (let [f #'util/time-cmp
            t0 {:hour 14, :minute 29}
            t1 {:hour 14, :minute 30}
            t2 {:hour 15, :minute 0}]
        
        (f > t1 t1) => false   ; equal
        (f > t1 t0) => true
        (f > t1 t2) => false
        
        (f >= t1 t1) => true   ; equal
        (f >= t1 t0) => true
        (f >= t1 t2) => false
        
        (f <= t1 t1) => true   ; equal
        (f <= t1 t0) => false
        (f <= t1 t2) => true
        ))

(fact "`valid-hours?`"
      (let [f #'util/valid-hours?]
        (f {}) => FALSEY
        (f {:open nil, :close nil}) => FALSEY
        (f {:open  {:hour 1, :minute 0}}) => FALSEY
        (f {:open  {:hour 1, :minute nil}
            :close {:hour 2, :minute 30}}) => FALSEY
        (f {:open  {:hour 1, :minute 0}
            :close {:hour 2, :minute 30}}) => TRUTHY
            ))

(fact "`open-at?`"
      (let [f util/open-at?
            hours-0 {:open {:hour 12, :minute 0}
                     :close {:hour 6, :minute 0}}
            hours-1 {:open {:hour 10, :minute 0}
                     :close {:hour 20, :minute 0}}
            hours-2 {:open {:hour 10, :minute 0}
                     :close {:hour 18, :minute 0}}

            week-of-hours [{:wday 0, :hours hours-0}
                           {:wday 1, :hours hours-1}
                           {:wday 2, :hours hours-2}]

            hours-map-before {:wday 1 :hour 9 :minute 0}
            hours-map-start  {:wday 1 :hour 10 :minute 0}
            hours-map-during {:wday 1 :hour 12 :minute 0}
            hours-map-end    {:wday 1 :hour 20 :minute 0}
            hours-map-after  {:wday 1 :hour 22 :minute 30}
            ]
        (f hours-map-before week-of-hours) => FALSEY
        (f hours-map-start  week-of-hours) => TRUTHY
        (f hours-map-during week-of-hours) => TRUTHY
        (f hours-map-end    week-of-hours) => FALSEY
        (f hours-map-after  week-of-hours) => FALSEY
        ))
