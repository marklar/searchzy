(ns searchzy.test.service.inputs
  (:use midje.sweet)
  (:require [searchzy.service.inputs :as inputs]))

(fact "`str->val`"
      (let [f inputs/str->val]
        (f "[1 2 3]" nil) => [1 2 3]
        (f "3" nil) => 3
        (f "" nil) => nil
        (f nil nil) => nil
        (f "3" 2) => 3
        (f "3.1" 2) => 3.1
        (f "" 2) => 2
        (f nil 2) => 2))

(fact "`true-str?`"
      (let [f inputs/true-str?]
        (f "false") => false
        (f "f") => false
        (f "0") => false
        (f "3") => false   ; for true, can only be '1'??
        (f "tr") => false  ; must match 't' or 'true'
        (f "1") => true
        (f "t") => true
        (f "true") => true))

(fact "`mk-page-map`"
      (let [f inputs/mk-page-map]
        (f {:from "3", :size "11"}) => {:from 3, :size 11}
        (f {:from "", :size ""}) => {:from 0, :size 10}
        (f {:from nil, :size "2"}) => {:from 0, :size 2}))

(fact "`mk-geo-map`"
      (let [f inputs/mk-geo-map]
        ;; Doesn't use geolocation, as it has the lat/lon already.
        (f {:address "Palo Alto"
            :lat "37"
            :lon "-122"
            :miles nil})
        => {:address {:input "Palo Alto"
                      :resolved nil}
            :coords {:lat 37, :lon -122}
            :miles 4.0}
        ;; Actually uses geolocation!!!!
        (f {:address "Palo Alto"
            :lat nil
            :lon nil
            :miles "3"})
        => {:address {:input "Palo Alto"
                      :resolved "Palo Alto, CA"}
            :coords {:lat 37.44466018676758, :lon -122.1607894897461}
            :miles 3}
        ;; Fails to geolocate.
        (f {:address nil
            :lat "37"
            :lon nil
            :miles nil})
        => nil))

;; TODO: validate ranges of values
(fact "`mk-hours-map`"
      (let [f #'inputs/mk-hours-map]
        (f "12:30") => nil
        (f "x:12:30") => nil
        (f "0:12:30") => {:wday 0, :hour 12, :minute 30}
        (f "0/12.30") => {:wday 0, :hour 12, :minute 30}))

(fact "`get-query`"
      (let [f inputs/get-query]
        (f "what-is-this") => "what is this"
        (f "what-IS-this?") => "what is this"
        (f "what is  this?") => "what is this"
        (f "What is this!?") => "what is this"
        (f nil) => nil
        (f "") => nil))

(fact "`clean-required-query`"
      (let [f inputs/clean-required-query
            error-map (fn [orig]
                        {:args {:normalized-query nil, :original-query orig}
                         :message "Param 'query' must be non-empty after normalization."
                         :param :query})]

        (f {:query "What is this?"}) => [{:query "what is this"}, nil]
        ;; TODO: Is this the desired behavior?
        (f {:query "=!!"}) => [{:query "="}, nil]

        (f {:query "--"}) => [{:query nil}, (error-map "--")]
        (f {:query ""})   => [{:query nil}, (error-map "")]
        (f {:query nil})  => [{:query nil}, (error-map nil)]
        ))

(fact "`clean-optional-query`"
      (let [f inputs/clean-optional-query]

        (f {:query "What is this?"}) => [{:query "what is this"}, nil]
        ;; TODO: Is this the desired behavior?
        (f {:query "=!!"}) => [{:query "="}, nil]

        (f {:query "--"}) => [{:query ""}, nil]
        (f {:query ""})   => [{:query ""}, nil]
        (f {:query nil})  => [{:query ""}, nil]
        ))

;; TODO: replace mk-geo-map with stub.
(fact "`clean-geo-map`"
      (let [f inputs/clean-geo-map]
        ;; Doesn't make use of geolocate.
        (f {:geo-map {:address ""
                      :lat "37", :lon "-122"}})
        => [{:geo-map {:address {:input "", :resolved nil}
                       :coords {:lat 37, :lon -122}
                       :miles 4.0}},
            nil]))

(fact "`clean-hours`"
      (let [f inputs/clean-hours]
        (f {:hours nil}) => [{:hours-map {}}
                             nil]
        (f {:hours "0/13:00"}) => [{:hours-map {:wday 0, :hour 13, :minute 0}}
                                   nil]
        (f {:hours "13:00"}) => [{:hours-map nil}
                                 {:param :hours
                                  :message "Some problem with the hours."
                                  :args "13:00"}]
        ))

;; TODO... clean-page-map
