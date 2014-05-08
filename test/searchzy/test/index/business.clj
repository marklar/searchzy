(ns searchzy.test.index.business
  (:use midje.sweet)
  (:require [searchzy.index
             [business :as biz]]))

;; To use a private function, precede namespace with "#'", like this:
;;   (#'some.ns/some-fn some args)

;; business
(fact "business addresses"
      (let [addr {:address_1 "2491 Aztec Way"
                  :address_2 "apt 2"
                  :city "Palo Alto"
                  :state "CA"
                  :zip "94303"}]
        (fact "`get-street` takes :address_1 and :address_2 and creates str"
              (#'biz/get-street addr) =>
              "2491 Aztec Way, apt 2")
        (fact "`get-address` ..."
              (#'biz/get-address addr) =>
              {:street "2491 Aztec Way, apt 2"
               :city "Palo Alto"
               :state "CA"
               :zip "94303"})))

(fact "`get-biz-hour-info`"
      (let [closed-day {:wday 0, :is_closed true,
                        :open_hour 10, :open_minute 0,
                        :close_hour 20, :close_minute 0}
            open-day (merge closed-day {:is_closed false})
            f #'biz/get-biz-hour-info]
        (f closed-day) => {:wday 0, :is_closed true}
        (f open-day) => {:wday 0
                         :hours {:open {:hour 10, :minute 0}
                                 :close {:hour 20, :minute 0}}}))

(fact "`get-coords`"
      (let [lat 30
            lon 20
            f #'biz/get-coords]
        (f {:coordinates [lon lat]}) => {:lat lat :lon lon}))

(fact "`get-lat-lon-str`"
      (let [f #'biz/get-lat-lon-str]
        (f {:coordinates [40.1 10.3]}) => "10.3,40.1"))

(fact "`get-value-score`"
      (let [f #'biz/get-value-score]
        (f [{:value_score 0.31} {} {:value_score 0.27}]) => 31
        (f [{}]) => 0))

(fact "`get-phone-number`"
      (let [f #'biz/get-phone-number]
        (f {:phone_country_code "1"
            :phone_area_code "650"
            :phone_number "555-1212"}) => "1-650-555-1212"))

;; {:description nil,
;;  :_id #<ObjectId 4fb687b4c9b1687205002954>,
;;  :active_ind false,
;;  :phone_number "3831515",
;;  :coordinates nil,
;;  :address_2 nil,
;;  :yelp_star_rating nil,
;;  :name "Fade 2 Famous",
;;  :permalink "fade-2-famous",
;;  :address_1 nil,
;;  :city nil,
;;  :value_score nil,
;;  :state nil,
;;  :updated_at #inst "2012-05-18T17:32:36.000-00:00",
;;  :zip nil,
;;  :phone_area_code "718",
;;  :seo_only_ind true,
;;  :url nil,
;;  :phone_country_code nil,
;;  :yelp_id nil,
;;  :business_category_ids [],
;;  :country nil,
;;  :created_at #inst "2012-05-18T17:32:36.000-00:00",
;;  :yelp_review_count nil,
;;  :citygrid_listing_id nil}

(fact "`mk-es-map`"
      (let [mg-map {:description nil
                    :_id "4fb687b4c9b1687205002954" ; #<ObjectId >
                    :active_ind false
                    :phone_number "3831515"
                    :coordinates [10.1 20.2]
                    :address_2 nil
                    :yelp_star_rating nil
                    :name "Fade 2 Famous"
                    :permalink "fade-2-famous"
                    :address_1 "123 Main St."
                    :city "Springfield"
                    :value_score 0.31 ; don't use this - get from embedded :business_items
                    :business_items '({:value_score 0.19} {:value_score nil})
                    :state "IL"
                    :updated_at "2012-05-18T17:32:36.000-00:00", ; #inst 
                    :zip "01234"
                    :phone_area_code "718"
                    :seo_only_ind true
                    :url nil
                    :phone_country_code nil
                    :yelp_id nil
                    :business_category_ids []
                    :country nil
                    :created_at "2012-05-18T17:32:36.000-00:00" ; #inst
                    :yelp_review_count nil
                    :citygrid_listing_id nil}]
        (biz/mk-es-map mg-map) => {:_id "4fb687b4c9b1687205002954"
                                   :name "Fade 2 Famous"
                                   :permalink "fade-2-famous"
                                   :yelp_star_rating nil
                                   :yelp_review_count nil
                                   :yelp_id nil
                                   :phone_number "718-3831515"
                                   :latitude_longitude "20.2,10.1"
                                   :business_category_ids '()
                                   :value_score_int 19
                                   :address {:street "123 Main St."
                                             :city "Springfield"
                                             :zip "01234"
                                             :state "IL"}
                                   :coordinates {:lat 20.2, :lon 10.1}
                                   :hours '()}))

(fact "`mk-query-map`"
      (let [f #'biz/mk-query-map]
        (f ..some-date.. ["1" "2"]) => {:updated_at {:$gte ..some-date..}
                                        :_id {:$in ["1" "2"]}}
        (f ..some-date.. nil) => {:updated_at {:$gte ..some-date..}}
        (f nil ["1" "2"]) => {:_id {:$in ["1" "2"]}}
        (f nil nil) => {}))

(fact "`mk-fetch-opts`"
      (let [f #'biz/mk-fetch-opts]
        (f 10 ..some-date.. ["1" "2"]) => '(:limit 10 :where {:updated_at {:$gte ..some-date..}
                                                              :_id {:$in ["1" "2"]}})
        (f nil ..some-date.. []) => '(:where {:updated_at {:$gte ..some-date..}})))
