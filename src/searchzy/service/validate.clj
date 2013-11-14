(ns searchzy.service.validate
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
    :params {:original-query orig-query
             :normalized-query norm-query}}))

(defn response-bad-location
  "The user has input insufficient location (lat/lon or address) info,
   so return a 404."
  [{:keys [address lat lon]}]
  (responses/error-json
   {:error "Must provide: (valid 'address' OR ('lat' AND 'lon'))."
    :params {:address address
             :lat lat
             :lon lon}}))

(defn response-bad-sort
  "The user has input an invalid 'sort' value, so return 404."
  [sort-str]
  (responses/error-json
   {:error "Invalid value for 'sort'."
    :sort sort-str}))
