(ns searchzy.test.index.business-menu-item
  (:use midje.sweet)
  (:require [searchzy.index
             [business-menu-item :as bmi]
             [business :as biz]]))

(def mg-map
  {:description ""
   :fdb_id "848953c4-49fa-b8b9-4001-0141d4697f5b"
   :_id "4fb47468bcd7ac4656000887" ; #<ObjectId >
   :email nil
   :active_ind true
   :business_items [{:_id "51a6254c6bddbbe98100115d" ; #<ObjectId >
                     :item_id "4fb4706cbcd7ac45cf000026" ; #<ObjectId >
                     :value_score 0.24415536818499417
                     :not_offered false
                     :synced_with_centzy_at "2013-05-29T15:57:00.455-00:00" ;#inst
                     :item_name "Manicure"
                     :price_score 0.48831073636998834
                     :updated_at "2013-05-29T15:57:00.455-00:00" ;#inst
                     :centzy_id "4faaf1d4c9b168640c002add" ;#<ObjectId >
                     :price 20.0
                     :created_at "2013-05-29T15:57:00.414-00:00" ;#inst
                     :quality_score 0.0
                     }
                    {:_id "51a6254c6bddbbe98100115e" ;#<ObjectId >
                     :item_id "4fb4706cbcd7ac45cf000028" ;#<ObjectId >
                     :value_score 0.21291742482646947
                     :not_offered false
                     :synced_with_centzy_at "2013-05-29T15:57:00.479-00:00" ;#inst
                     :item_name "Pedicure"
                     :price_score 0.42583484965293894
                     :updated_at "2013-05-29T15:57:00.480-00:00" ;#inst
                     :centzy_id "4faaf1d4c9b168640c002adf" ;#<ObjectId >
                     :price 30.0
                     :created_at "2013-05-29T15:57:00.458-00:00" ;#inst
                     :quality_score 0.0
                     }
                    {:_id "51a6254c6bddbbe98100115f" ;#<ObjectId >
                     :item_id "4fb4706cbcd7ac45cf00002a" ;#<ObjectId >
                     :value_score 0.25015463640864116
                     :not_offered false
                     :synced_with_centzy_at "2013-05-29T15:57:00.498-00:00" ;#inst
                     :item_name "Mani-Pedi"
                     :price_score 0.5003092728172823
                     :updated_at "2013-05-29T15:57:00.498-00:00" ;#inst
                     :centzy_id "4faaf1d4c9b168640c002ae1" ;#<ObjectId >
                     :price 50.0
                     :created_at "2013-05-29T15:57:00.481-00:00" ;#inst
                     :quality_score 0.0
                     }]
   :phone_number "3263643"
   :coordinates [-122.18377 37.45403]
   :has_merchant_premium_account false
   :unified_menu {:_id "5272d9f02719f7a8b1000f76" ;#<ObjectId >
                  :name ""
                  :description ""
                  :fdb_id "2b11fd36-49fa-b8b9-4001-0141d4697f61"
                  :updated_at "2013-10-31T22:30:08.656-00:00" ;#inst
                  :created_at "2013-10-31T22:30:08.656-00:00" ;#inst
                  :sections [{:_id "5272d9f02719f7a8b1000f77" ;#<ObjectId >
                              :name "Mani-Pedi"
                              :description ""
                              :fdb_id "0fd7bdaa-49fa-b8b9-4001-0141d4697f88"
                              :item_category_id "4fb4706cbcd7ac45cf000029" ;#<ObjectId >
                              :items [{:_id "5272d9f02719f7a8b1000f78" ;#<ObjectId >
                                       :name "Mani-Pedi"
                                       :description ""
                                       :fdb_id "b31c60f9-49fa-b8b9-4001-0141d4697f7d"
                                       :item_id "4fb4706cbcd7ac45cf00002a" ;#<ObjectId >
                                       :value_score_picos 250154636408
                                       :item_price_type 2
                                       :price_micros 50000000
                                       :item_price_currecy_fdb_id 0}]}
                             {:_id "5272d9f02719f7a8b1000f79" ;#<ObjectId >
                              :name "Pedicure"
                              :description ""
                              :fdb_id "68402339-49fa-b8b9-4001-0141d4697f7c"
                              :item_category_id "4fb4706cbcd7ac45cf000027" ;#<ObjectId >
                              :items [{:_id "5272d9f02719f7a8b1000f7a" ;#<ObjectId >
                                       :name "Pedicure"
                                       :description ""
                                       :fdb_id "1d74d578-49fa-b8b9-4001-0141d4697f71"
                                       :item_id "4fb4706cbcd7ac45cf000028" ;#<ObjectId >
                                       :value_score_picos 212917424826
                                       :item_price_type 2
                                       :price_micros 30000000
                                       :item_price_currecy_fdb_id 0}]}
                             {:_id "5272d9f02719f7a8b1000f7b" ;#<ObjectId >
                              :name "Manicure"
                              :description ""
                              :fdb_id "c1b888b7-49fa-b8b9-4001-0141d4697f70"
                              :item_category_id "4fb4706cbcd7ac45cf000025" ;#<ObjectId >
                              :items [{:_id "5272d9f02719f7a8b1000f7c" ;#<ObjectId >
                                       :name "Manicure"
                                       :description ""
                                       :fdb_id "76ec3bf6-49fa-b8b9-4001-0141d4697f61"
                                       :item_id "4fb4706cbcd7ac45cf000026" ;#<ObjectId >
                                       :value_score_picos 244155368184
                                       :item_price_type 2
                                       :price_micros 20000000
                                       :item_price_currecy_fdb_id 0}]}]},
   :address_2 ""
   :zip_plus_4 4307
   :yelp_star_rating 0.0
   :has_special_ind false
   :name "Price Carolyn Manicurist"
   :permalink "price-carolyn-manicurist-menlo-park-ca"
   :address_1 "1183 El Camino Real"
   :localeze_pid 10733767
   :city "Menlo Park"
   :state "CA"
   :synced_with_centzy_at "2013-05-29T15:57:00.512-00:00" ;#inst
   :country_fdb_id "6d5667b1-1cad-b889-f601-0141c8f7f0dd"
   :updated_at "2013-10-31T22:30:08.674-00:00" ;#inst
   :locality_fdb_id "0c9868cb-1d0e-b889-f601-0141c8fb15cb"
   :hide_yelp_content false
   :street_fdb_id "fdfb400e-1d0e-b889-f601-0141c8fb15eb"
   :merchant_appointment_enabled false
   :zip "94025"
   :rails_time_zone "Pacific Time (US & Canada)"
   :merchant_click_to_call_enabled false
   :region_fdb_id "7b97fec7-1cad-b889-f601-0141c8f7f0f4"
   :phone_area_code "650"
   :has_merchant_account false
   :seo_only_ind false
   :url nil
   :phone_country_code "1"
   :merchant_lead_gen_enabled false
   :centzy_id "4f9efd44c9b1688dc70009d9" ;#<ObjectId >
   :booker_appointment_enabled false
   :yelp_id "price-carolyn-manicurist-menlo-park"
   :business_category_ids ["4fb4706bbcd7ac45cf000019"] ;#<ObjectId >
   :centzy_data_scrape_id "4fe23d7e05b3c531690000f9" ;#<ObjectId >
   :country "US"
   :data_source_name "localeze"
   :created_at "2012-05-17T03:45:44.000-00:00" ;#inst
   :merchant_appointment_testing_enabled false
   :yelp_review_count 0
   :citygrid_listing_id "616391260"
   :force_use_menu false})


(def items
  '({:_id "5272d9f02719f7a8b1000f78" ;#<ObjectId >
     :name "Mani-Pedi"
     :description ""
     :fdb_id "b31c60f9-49fa-b8b9-4001-0141d4697f7d"
     :item_id "4fb4706cbcd7ac45cf00002a" ;#<ObjectId >
     :value_score_picos 250154636408
     :item_price_type 2
     :price_micros 50000000
     :item_price_currecy_fdb_id 0}
    {:_id "5272d9f02719f7a8b1000f7a" ;#<ObjectId >
     :name "Pedicure"
     :description ""
     :fdb_id "1d74d578-49fa-b8b9-4001-0141d4697f71"
     :item_id "4fb4706cbcd7ac45cf000028" ;#<ObjectId >
     :value_score_picos 212917424826
     :item_price_type 2
     :price_micros 30000000
     :item_price_currecy_fdb_id 0}
    {:_id "5272d9f02719f7a8b1000f7c" ;#<ObjectId >
     :name "Manicure"
     :description ""
     :fdb_id "76ec3bf6-49fa-b8b9-4001-0141d4697f61"
     :item_id "4fb4706cbcd7ac45cf000026" ;#<ObjectId >
     :value_score_picos 244155368184
     :item_price_type 2
     :price_micros 20000000
     :item_price_currecy_fdb_id 0
     }))

;;------------------------------

(fact "`get-items-from-mg-map`"
      (let [f #'bmi/get-items-from-mg-map]
        (f mg-map) => items))

(fact "`mk-es-item-map`"
      (let [f #'bmi/mk-es-item-map]
        (f {:item_id nil}) => nil
        (f (first items)) => {:_id "5272d9f02719f7a8b1000f78"
                              :fdb_id "b31c60f9-49fa-b8b9-4001-0141d4697f7d"
                              :name "Mani-Pedi"
                              :item_id "4fb4706cbcd7ac45cf00002a"
                              :price_micros 50000000
                              :value_score_picos 250154636408}))

(fact "`mk-es-maps`"
      (let [f #'bmi/mk-es-maps
            biz-es-map (biz/mk-es-map mg-map)]
        (count (f mg-map)) => 3
        (first (f mg-map)) => {:yelp_star_rating 0.0
                               :yelp_review_count 0
                               :latitude_longitude (:latitude_longitude biz-es-map)
                               :business biz-es-map
                               :_id "5272d9f02719f7a8b1000f78"
                               :fdb_id "b31c60f9-49fa-b8b9-4001-0141d4697f7d"
                               :name "Mani-Pedi"
                               :item_id "4fb4706cbcd7ac45cf00002a"
                               :price_micros 50000000
                               :value_score_picos 250154636408}))
