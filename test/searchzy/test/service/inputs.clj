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
        (f true) => true
        (f false) => false
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

(fact "`clean-page-map`"
      (let [f inputs/clean-page-map]
        (f {}) => [{:page-map {:from 0, :size 10}}, nil]
        (f {:page-map {:from "20", :size "5"}}) => [{:page-map {:from 20, :size 5}}, nil]
        (f {:page-map {:from nil, :size "5"}})  => [{:page-map {:from 0, :size 5}}, nil]
        (f {:page-map {:from nil, :size nil}})  => [{:page-map {:from 0, :size 10}}, nil]
        ))

(fact "`clean-include-unpriced`"
      (let [f inputs/clean-include-unpriced]
        (f {:include-unpriced true}) => [{:include-unpriced true}, nil]
        (f {}) => [{:include-unpriced false}, nil]
        (f nil) => [{:include-unpriced false}, nil]
        (f {:include-unpriced "t"}) => [{:include-unpriced true}, nil]
        (f {:include-unpriced "true"}) => [{:include-unpriced true}, nil]
        (f {:include-unpriced "f"}) => [{:include-unpriced false}, nil]
        (f {:include-unpriced "false"}) => [{:include-unpriced false}, nil]
        ))

(fact "`clean-html`"
      (let [f inputs/clean-html]
        (f {}) => [{:html false}, nil]
        (f {:html ""}) => [{:html false}, nil]
        (f {:html nil}) => [{:html false}, nil]
        (f {:html "f"}) => [{:html false}, nil]
        (f {:html "false"}) => [{:html false}, nil]
        (f {:html "t"}) => [{:html true}, nil]
        (f {:html "true"}) => [{:html true}, nil]
        ))

(fact "`get-order-and-attr`"
      (let [f #'inputs/get-order-and-attr]
        (f "value") => [:asc "value"]
        (f "-value") => [:desc "value"]
        (f " -value ") => [:desc "value"]
        (f "distance") => [:asc "distance"]
        ))

;; TODO: make attributes keywords (instead of strings)?
(fact "`get-sort-map`"
      (let [f #'inputs/get-sort-map]
        (f "value" #{"value" "distance"}) => {:attribute "value", :order :asc}
        (f "-value" #{"value" "distance"}) => {:attribute "value", :order :desc}
        (f "blurfl" #{"value" "distance"}) => nil
        ))

(fact "`clean-sort`"
      (let [f (inputs/clean-sort #{"value" "distance"})]
        (f {:sort "-value"}) => [{:sort-map {:attribute "value", :order :desc}}, nil]
        (f {:sort "value"}) => [{:sort-map {:attribute "value", :order :asc}}, nil]
        (f {:sort "blurfl"})
        => [{:sort-map nil}
            {:args "blurfl"
             :message "Invalid value for param 'sort'."
             :options '("value" "-value" "distance" "-distance")
             :param :sort}]
        ))

(fact "`str-or-nil`"
      (let [f #'inputs/str-or-nil]
        (f nil) => nil
        (f "") => nil
        (f "non-empty") => "non-empty"
        ))

(fact "`clean-item-id`"
      (let [f inputs/clean-item-id
            error-map {:param :item_id
                       :message "Param 'item_id' must have non-empty value."}]
        (f {:item-id nil}) => [{:item-id nil}, error-map]
        (f {:item-id ""})  => [{:item-id nil}, error-map]
        (f {:item-id "1234"})  => [{:item-id "1234"}, nil]
        ))

(fact "`clean-business-category-ids`"
      (let [f inputs/clean-business-category-ids]
        (f {:business-category-ids ""})
        => [{:business-category-ids []}, nil]
        (f {:business-category-ids nil})
        => [{:business-category-ids []}, nil]
        (f {:business-category-ids "1234"})
        => [{:business-category-ids ["1234"]}, nil]
        (f {:business-category-ids "1234,5678"})
        => [{:business-category-ids ["1234" "5678"]}, nil]
        ))

(fact "`get-utc-offset-map`"
      (let [f inputs/get-utc-offset-map]
        ;; wrong
        (f "blurfl") => nil
        (f "38js.")  => nil
        ;; optional
        (f nil)      => {}
        (f "")       => {}
        ;; good
        (f "-12")    => {:hours -12, :minutes 0}
        (f "+1")     => {:hours 1,   :minutes 0}
        (f "+5:45")  => {:hours 5,   :minutes 45}
        ))

(fact "`clean-utc-offset`"
      (let [f inputs/clean-utc-offset]
        ;; wrong
        (f {:utc-offset "blurfl"})
        => [{:utc-offset-map nil},
            {:param :utc_offset
             :message "'utc_offset' should have values like '-5' or '+5:45'."
             :args "blurfl"}]
        ;; optional
        (f {:utc-offset ""})   => [{:utc-offset-map {}}, nil]
        ;; okay
        (f {:utc-offset "+1"}) => [{:utc-offset-map {:hours 1, :minutes 0}}, nil]
        ))

;;--------------

(let [good-input-geo-map {:lat "60", :lon "-120"}
      default-page-map   {:from 0, :size 10}
      default-sort-map   {:attribute "value", :order :desc}]

  (fact "`lists-clean-input`"
        (let [f inputs/lists-clean-input
              no-errors ()]

          ;; This uses geolocate!  No page-map.
          ;; (f {:geo-map {:address "123 Main St", :miles "5"}})
          ;; => [{:geo-map {:address {:input "123 Main St"
          ;;                          :resolved "123 Main St, Haines, AK 99827"}
          ;;                :coords {:lat 59.23487953841686, :lon -135.4442422091961}
          ;;                :miles 5}
          ;;      :page-map default-page-map}
          ;;     no-errors]

          ;; Providing lat/lon.  Partial page-map.
          (f {:geo-map good-input-geo-map
              :page-map {:from "3"}})
          => [{:geo-map {:address {:input nil, :resolved nil}
                         :coords {:lat 60, :lon -120}
                         :miles 4.0}
               :page-map {:from 3, :size 10}}
              no-errors]

          ;; Bad geo-map.
          (f {:geo-map {:lat "30"}})
          => [{:geo-map nil, :page-map default-page-map},
              ;; list of one error
              '({:params [:address :lat :lon]
                 :message "Must provide: (valid 'address' OR ('lat' AND 'lon'))."
                 :args {:lat "30"}})]

          ))

  (fact "`business-clean-input`"
        (let [f inputs/business-clean-input]

          ;; No input: geo-map problem.
          (f {})
          => [{:query ""
               :business-category-ids []
               :geo-map nil   ;; <- problem
               :hours-map {}
               :utc-offset-map {}
               :sort-map default-sort-map
               :page-map default-page-map}
              ;; list of one error
              '({:args nil
                 :message "Must provide: (valid 'address' OR ('lat' AND 'lon'))."
                 :params [:address :lat :lon]})
              ]

          ;; No geo-map, and strange hours-map.
          (f {:hours "12:00"})  ; missing wday
          => [{:query ""
               :business-category-ids []
               :geo-map nil   ;; <- problem
               :hours-map nil
               :utc-offset-map {}
               :sort-map default-sort-map
               :page-map default-page-map}
              ;; list of two errors (ORDER MATTERS)
              '(
                {:param :hours
                 :message "Some problem with the hours."
                 :args "12:00"}
                {:params [:address :lat :lon]
                 :message "Must provide: (valid 'address' OR ('lat' AND 'lon'))."
                 :args nil}                
                )
              ]

          ;; Defaults for all but geo-map.
          (f {:geo-map {:lat "30", :lon "-120"}})
          => [{:query ""
               :business-category-ids []
               :geo-map {:address {:input nil, :resolved nil}
                         :coords {:lat 30, :lon -120}
                         :miles 4.0}
               :hours-map {}
               :utc-offset-map {}
               :sort-map default-sort-map
               :page-map default-page-map}
              ;; no errors
              ()]
                                                      
          ))

  (fact "`biz-menu-item-clean-input`"
        (let [f inputs/biz-menu-item-clean-input]
          ;; No input - requires both geo-map and item-id.
          (f {})
          => 
          [{:geo-map nil  ; <- problem
            :hours-map {}
            :include-unpriced false
            :item-id nil  ; <- problem
            :page-map default-page-map
            :sort-map default-sort-map
            :utc-offset-map {}}
           '({:args nil
              :message "Must provide: (valid 'address' OR ('lat' AND 'lon'))."
              :params [:address :lat :lon]}
             {:message "Param 'item_id' must have non-empty value."
              :param :item_id})]

          ;; Defaults for all but geo-map and item-id.
          (f {:item-id "1234"
              :geo-map {:lat "30", :lon "-120"}})
          => [{:item-id "1234"
               :geo-map {:address {:input nil, :resolved nil}
                         :coords {:lat 30, :lon -120}
                         :miles 4.0}
               :hours-map {}
               :include-unpriced false
               :utc-offset-map {}
               :sort-map default-sort-map
               :page-map default-page-map}
              ;; no errors
              ()]
          ))

  (fact "`suggestion-clean-input-v1`"
        (let [f inputs/suggestion-clean-input-v1]
          ;; Lacking query and geo-map.  (Blank query not okay.)
          (f {})
          => [{:query nil    ; <- problem
               :geo-map nil  ; <- problem
               :business-category-ids []
               :html false
               :page-map {:from 0, :size 10}}
              ;; Errors - order matters (FIFO).
              '({:args nil
                 :message "Must provide: (valid 'address' OR ('lat' AND 'lon'))."
                 :params [:address :lat :lon]}
                {:args {:normalized-query nil, :original-query nil}
                 :message "Param 'query' must be non-empty after normalization."
                 :param :query})]
          ))

  (fact "`suggestion-clean-input-v2`"
        (let [f inputs/suggestion-clean-input-v2]
          ;; Lacking geo-map.  (Blank query IS okay.)
          ;; Unlike v1, includes optional utc-offset-map.
          (f {})
          => [{:query ""
               :geo-map nil  ; <- problem
               :utc-offset-map {}
               :business-category-ids []
               :html false
               :page-map {:from 0, :size 10}}
              ;; Errors - order matters (FIFO).
              '({:args nil
                 :message "Must provide: (valid 'address' OR ('lat' AND 'lon'))."
                 :params [:address :lat :lon]})]
          ))

)
