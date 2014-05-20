(ns searchzy.test.service.mean-prices
  (:use midje.sweet)
  (:require [searchzy.service.mean-prices :as mean-prices]))

;; (fact "`mk-bool-locs-query`"
;;       (let [f #'mean-prices/mk-bool-locs-query
;;             loc-id "53202e1c3bf915bb6e000001"]
;;         (f loc-id) => {:bool {:should {:term {:_id loc-id}}}}))
