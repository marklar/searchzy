(ns searchzy.search.business.validate
  (:require [searchzy.search
             [util :as util]]))

(defn valid-sort?
  "Has the user input a legal value for 'sort'?"
  [sym]
  ;; FIXME: We may have more sort opts than just #{'value 'lexical}.
  (contains? #{'value 'lexical} sym))

(defn invalid-location?
  "Has the user provided _IN_sufficient location information?"
  [address lat lng]
  (and (clojure.string/blank? address)
       (or (nil? lat) (nil? lng))))

(defn response-bad-query
  "The user has input an invalid query, so we return 404."
  [orig-query norm-query]
  (util/error-json-response
   {:error "Param 'query' must be non-empty after normalization."
    :original-query orig-query
    :normalized-query norm-query}))

(defn response-bad-location
  "The user has input insufficient location (lat/lng or address) info,
   so return a 404."
  [address orig-lat orig-lng]
  (util/error-json-response
   {:error "Must provide EITHER address OR both lat & lng."
    :address address
    :lat orig-lat
    :lng orig-lng}))

(defn response-bad-sort
  "The user has input an invalid 'sort' value, so return 404."
  [sort]
  (util/error-json-response
   {:error "Param 'sort' must be: 'value', 'lexical', or absent."
    :sort sort}))
