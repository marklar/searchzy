(ns searchzy.test.index.util
  (:use midje.sweet)
  (:require [searchzy.index
             [util :as util]]))

(fact "`file->lines` returns seq of strs"
      (util/file->lines "nada.csv") => '("foo" "bar")
      (provided (slurp "nada.csv") => "foo\nbar"))

(fact "`file->lines` throws when given nil or bogus file"
      (util/file->lines nil) => (throws Exception)
      (util/file->lines "blurfl.csv") => (throws Exception))

