(ns searchzy.service.business.validate
  (:require [clojure.string :as str]
            [searchzy.service.responses :as responses]))

;; -- Validations --

(defn valid-sort?
  "Has the user input a legal value for 'sort'?"
  [sym]
  ;; FIXME: We may have more sort opts than just #{'value 'lexical}.
  (contains? #{'value 'lexical} sym))

(defn invalid-location?
  "Has the user provided _IN_sufficient location information?"
  [address lat lon]
  (and (str/blank? address)
       (or (nil? lat) (nil? lon))))

;; -- Negative responses --

(defn response-bad-query
  "The user has input an invalid query, so we return 404."
  [orig-query norm-query]
  (responses/error-json
   {:error "Param 'query' must be non-empty after normalization."
    :original-query orig-query
    :normalized-query norm-query}))

(defn response-bad-location
  "The user has input insufficient location (lat/lon or address) info,
   so return a 404."
  [address orig-lat orig-lon]
  (responses/error-json
   {:error "Must provide EITHER address OR both lat & lon."
    :address address
    :lat orig-lat
    :lon orig-lon}))

(defn response-bad-sort
  "The user has input an invalid 'sort' value, so return 404."
  [sort]
  (responses/error-json
   {:error "Param 'sort' must be: 'value', 'lexical', or absent."
    :sort sort}))
