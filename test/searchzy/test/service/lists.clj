(ns searchzy.test.service.lists
  (:use midje.sweet)
  (:require [searchzy.service.lists :as lists]))

(fact "`mk-bool-term-query`"
      (let [f #'lists/mk-bool-term-query]
        (f {}) => nil
        (f {:location-id "foo"
            :seo-business-category-id "bar"})
        => {:bool {:must [ {:term {:location_id "foo"}}
                           {:term {:seo_business_category_id "bar"}} ] }}

        ))
