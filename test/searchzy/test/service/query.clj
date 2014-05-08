(ns searchzy.test.service.query
  (:use midje.sweet)
  (:require [searchzy.service.query :as query]))

(fact "`double-quote-each-char`"
      (let [f #'query/double-quote-each-char]
        (f nil) => ""
        (f "") => ""
        (f "abc") => "\\a\\b\\c"
        ))

;; unused
(fact "`escape-special-chars`"
      (let [f #'query/escape-special-chars]
        (f "") => ""
        (f "abc") => "abc"
        (f "\\abc") => "\\\\abc"
        (f "a+b")   => "a\\+b"
        (f "?:^")   => "\\?\\:\\^"
        ))

(fact "`whitespace-special-chars`"
      (let [f #'query/whitespace-special-chars]
        (f "") => ""
        (f "abc") => "abc"
        (f "\\abc") => " abc"
        (f "a+b")   => "a b"
        (f "?:^")   => "   "
        ))

(fact "`count-char`"
      (let [f #'query/count-char]
        (f \c "") => 0
        (f \c nil) => 0
        (f \c "c") => 1
        (f \c "abcdefghijabcde") => 2
        ))

(fact "`escape-double-quotes`"
      (let [f #'query/escape-double-quotes]
        (f nil)         => (throws Exception)
        (f "")          => ""
        (f "a\"bc")     => "a\\\"bc"
        (f "a\"bc\"de") => "a\\\"bc\\\"de"
        ))

(fact "`escape-odd-double-quotes`"
      (let [f #'query/escape-odd-double-quotes]
        (f nil)       => nil
        (f "")        => ""
        (f "a\"bc")   => "a\\\"bc"   ; <- odd
        (f "a\"b\"c") => "a\"b\"c"   ; <- even
        ))

(fact "`normalize`"
      (let [f query/normalize]
        (f nil) => nil
        (f "")  => ""
        (f "---- z") => "z"
        (f " Don't \"qUoTe\"! these -double quotes ! ") => "don't \"quote\" these double quotes"
        (f " What shoUld\"! this -look like ? ") => "what should\\\" this look like"
        ))
