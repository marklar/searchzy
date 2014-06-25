(ns searchzy.service.bmis.businesses
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [geo-sort :as geo-sort]
             [geo-util :as geo-util]
             [util :as util]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

;; Rather than search the 'business_menu_items' index,
;; we need to search the 'businesses' index.
(defn- es-search
  "Perform search against BusinessCategory _id, specifically using:
     - geo-distance sort  -AND-
     - geo-distance filter"
  [cat-id merch-appt geo-map page-map]
  (let [names        (:businesses cfg/elastic-search-names)
        geo-filter   (geo-util/mk-geo-filter geo-map)
        merch-filter (util/mk-merch-appt-filter merch-appt)
        filters      (util/compact [geo-filter merch-filter])]
    (:hits
     (es-doc/search (:index names) (:mapping name)
                    :query {:filtered {:query {:term {:business_category_ids cat-id}}
                                       :filter {:and filters}}}
                    :sort  (geo-sort/mk-geo-distance-sort-builder
                            (:coords geo-map) :asc)
                    :from  (:from page-map)
                    :size  (:size page-map)))))

;;-------------------------

(defn for-category
  "Given business_category_id,
   search ElasticSearch for corresponding Businesses.
   Doesn't return BusinessMenuBmis, mind you -- just Businesses."
  [biz-cat-id merch-appt geo-map pager]
  (if (nil? biz-cat-id)
    []
    (let [{bizs :hits} (es-search biz-cat-id merch-appt geo-map pager)]
      bizs)))

(defn ->bmi
  [biz]
  (let [just-biz (:_source biz)]
    {:business (assoc just-biz :_id (:_id biz))}))
