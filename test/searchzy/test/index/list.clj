(ns searchzy.test.index.list
  (:use midje.sweet)
  (:require [searchzy.index
             [list :as list]]))

(fact "`coords->str`"
      (let [f #'list/coords->str]
        (f [30.3 20.2]) => "20.2,30.3"))

(fact "`mg->es`"
      (let [f #'list/mg->es
            mg-map {:_id "52f2dea8f1d37f36a6006af4" ;#<ObjectId >
                    :postal_code nil
                    :active_ind true
                    :coordinates [-73.98994069999999 40.7412875]
                    :top_list_permalink "wax-flatiron"
                    :area_permalink "flatiron"
                    :neighborhood_id nil
                    :name "Flatiron"
                    :permalink "nyc/wax-flatiron"
                    :city "Manhattan"
                    :seo_item_id "4fc66d40c9b16860e200003f" ;#<ObjectId >
                    :state "NY"
                    :legacy_area_top_list_id "4fc7c4ecc9b168870e00001a" ;#<ObjectId >
                    :updated_at "2014-02-06T01:00:24.258-00:00" ;#inst
                    :area_type "neighborhood"
                    :keyword nil
                    :seo_region_id "4fc6aba5c9b16867c7000001" ;#<ObjectId >
                    :high_quality_ind false
                    :legacy_area_id "4fb4fb6fc9b16853d3000014" ;#<ObjectId >
                    :created_at "2014-02-06T01:00:24.258-00:00"  ;#inst
                    }]
        (f mg-map) => (contains {:latitude_longitude "40.7412875,-73.98994069999999"
                                 :city "Manhattan"})))
