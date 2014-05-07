(ns searchzy.test.util
  (:use midje.sweet)
  (:require [searchzy.util :as util]))

(fact "`maybe-take`"
      (let [f util/maybe-take]
        (f 10 [1 2 3]) => [1 2 3]
        (f 2 [1 2 3]) => [1 2]
        (f nil [1 2 3]) => [1 2 3]))

(fact "`doseq-cnt`"
      (let [f util/doseq-cnt]
        (f inc 2 [0 1 2 3 4 5 6 7 8 9 10 11 12 13]) => 14
        (provided (println anything) => "foo" :times 7)))

(fact "`auth-str`"
      (let [f #'util/auth-str]
        (f "user" "pass") => "user:pass@"
        (f "user" nil) => "user@"
        (f nil "pass") => ""
        (f nil nil) => ""))

(fact "`mk-conn-str`"
      (let [f util/mk-conn-str]
        (f {:db-name "db" :host "host" :port "80" :username "user" :password "pass"})
          => "mongodb://user:pass@host:80/db"
        (f {:db-name "db" :host "host" :username "user"})
          => "mongodb://user@host/db"))


