(ns searchzy.service.bmis.hits
  (:require [camel-snake-kebab :as case]
            [searchzy.service
             [util :as util]
             [responses :as responses]]))

(defn- mk-one-hit
  "Replace :hours with :hours_today, using :day_of_week.
   Add in Yelp data."
  [result day-of-week]
  (let [source-map  result
        old-biz     (:business source-map)
        hours-today (util/get-hours-for-day (:hours old-biz) day-of-week)
        new-biz     (-> old-biz
                        (dissoc :hours
                                :latitude_longitude
                                :rails_time_zone
                                :yelp_star_rating
                                :yelp_review_count
                                :yelp_id)
                        (assoc
                            :yelp {:id (:yelp_id old-biz)
                                   :star_rating (:yelp_star_rating old-biz)
                                   :review_count (:yelp_review_count old-biz)}
                            :hours_today hours-today
                            ))]
    (-> source-map
        (dissoc :latitude_longitude
                :yelp_star_rating
                :yelp_review_count)
        (assoc :business new-biz
               :awesomeness (:awesomeness result)))))

(defn- map-keys
  "Recursive, but only when digging into sub-maps, and it won't go deep."
  [f m]
  (letfn [(mapper [[k v]] [(f k) (if (map? v) (map-keys f v) v)])]
    (into {} (map mapper m))))

;;-----------------------

(defn mk-response
  "From ES response, create service response.
   We haven't done paging yet (because we needed metadata),
   so we need to do paging here."
  [results metadata day-of-week {:keys [ item-id geo-map hours-map
                                         utc-offset-map sort-map page-map
                                         include-unpriced ]}]
  (let [pageful   (take (:size page-map) (drop (:from page-map) results))
        resp-hits (map #(mk-one-hit % day-of-week) pageful)]
    (responses/ok-json
     {:endpoint "/v1/business_menu_items"   ; TODO: pass this in
      :arguments {:item_id item-id
                  :include_unpriced include-unpriced
                  :geo_filter geo-map
                  :hours_filter hours-map
                  :utc_offset utc-offset-map
                  :day_of_week day-of-week
                  :sort sort-map
                  :paging page-map}
      :results {:count (count results)
                :prices_micros (:prices-micros metadata)
                :latest_close (:latest-close metadata)
                :hits resp-hits
                }})))
