(ns searchzy.test.service.bmis.metadata
  (:use midje.sweet)
  (:require [searchzy.service.bmis.metadata :as metadata]))

(fact "`get-price-meta`"
      (let [f #'metadata/get-price-meta]
        (f [])      => {:mean 0, :max 0, :min 0}   ;; return nil instead?
        (f [3 2 4]) => {:mean 3, :max 4, :min 2}))

(fact "`mins->hour`"
      (let [f #'metadata/mins->hour]
        (f 0)    => {:hour 0,  :minute 0}
        (f 300)  => {:hour 5,  :minute 0}
        (f 1230) => {:hour 20, :minute 30}))

(fact "`get-latest-hour`"
      (let [f #'metadata/get-latest-hour]
        ;; No hours info...
        (f []) => {:hour 0, :minute 0}  ;; return nil instead?
        ;; Some...
        (f [{:hour 20, :minute 30}
            {:hour 18, :minute 0}
            {:hour 19, :minute 45}])
        => {:hour 20, :minute 30}))
        
            
