(ns searchzy.search.business-validate
  (:require [searchzy.search
             [util :as util]]))

(defn valid-sort?
  [sym]
  ;; FIXME: We may have more sort opts than just #{'value 'lexical}.
  (contains? #{'value 'lexical} sym))

(defn invalid-location?
  [address lat lng]
  (and (clojure.string/blank? address)
       (or (nil? lat) (nil? lng))))

(defn response-bad-query
  [orig-query norm-query]
  (util/error-json-response
   {:error "Param 'query' must be non-empty after normalization."
    :original-query orig-query
    :normalized-query norm-query}))

(defn response-bad-location
  [address orig-lat orig-lng]
  (util/error-json-response
   {:error "Must provide EITHER address OR both lat & lng."
    :address address
    :lat orig-lat
    :lng orig-lng}))

(defn response-bad-sort
  [sort]
  (util/error-json-response
   {:error "Param 'sort' must be: 'value', 'lexical', or absent."
    :sort sort}))
