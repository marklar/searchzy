(ns searchzy.service.query)
;; Query normalization.

(defn -double-quote-each-char
  [s]
  (apply str (map #(str "\\" %) s)))

(def -pattern-str
  ;; "\\+-&|!(){}[]^~*?:"
  (str "[" (-double-quote-each-char "\\+-&|!(){}[]^~*?:") "]"))

(defn -escape-special-chars
  ;; Rather than escaping these characters, perhaps instead
  ;; we want to replace them with spaces?
  [s]
  (.replaceAll s -pattern-str "\\\\$0"))

(defn -count-char
  "Return number of characters 'ch' in string 's'."
  [ch s]
  (count (filter #(= ch %) s)))

(defn -escape-double-quotes
  [s]
  (.replaceAll s "(.*)\"(.*)" "$1\\\\\"$2"))

(defn -escape-odd-double-quotes
  "Count the double quotes in string 's'.
   Iff there's an odd number, escape all quotes."
  [s]
  (if (even? (-count-char \" s))
    s
    (-escape-double-quotes s)))

(defn normalize
  "0. Down-case.
   1. Escape special chars.
   2. Escape odd double quotes.
   (Escaping bool operators isn't necessary.
   The str is already downcased, and Lucene bool ops must be SHOUTED.)
   3. Trim."
  [str]
  (if (clojure.string/blank? str)
    str
    (-> str
        clojure.string/lower-case
        ;; TODO: Why do we want to escape special chars?
        ;; -escape-special-chars
        -escape-odd-double-quotes
        clojure.string/trim)))

