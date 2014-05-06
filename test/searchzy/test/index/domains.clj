(ns searchzy.test.index.domains
  (:use midje.sweet)
  (:require [searchzy.index
             [domains :as domains]]))

;; To use a private function, precede namespace with "#'", like this:
;;   (#'some.ns/some-fn some args)


;; domains
(fact "`name->index` returns an index hashmap"
      (let [f domains/name->index]
        (f :lists) => (contains {:db-name :areas})
        (keys (f :lists)) => (contains [:db-name :index-fn])))

(fact "`words` returns a seq of words"
      (let [f #'domains/words]
        (f "hubo un rey") => '("hubo" "un" "rey")
        (f "1, 2, 3 4 5, 6 7") => '("1" "2" "3" "4" "5" "6" "7")
        (f "1,2,3 4 5,6,7") => '("1" "2" "3" "4" "5" "6" "7")))

(fact "`str->name`"
      (let [f domains/str->names]
        (f "items, lists") => '(:items :lists)
        (f "biz-menu-items") => '(:biz-menu-items)
        (f "all") => domains/all-domains))
