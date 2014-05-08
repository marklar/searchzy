(ns searchzy.test.service.geo-locate
  (:use midje.sweet)
  (:require [searchzy.service.geo-locate :as geo-locate]))

(fact "`canonicalize-address`"
      (let [f #'geo-locate/canonicalize-address]
        (f "123  Main St., Springfield,  IL! ") => "123 main st springfield il"
        ))

(fact "`geo-locate/get-provider`"
      (let [f #'geo-locate/get-provider
            cfg {:geocoding {:provider " Bing "}}
            ]
        (f nil) => "google"
        (f cfg) => "bing"
        ))
