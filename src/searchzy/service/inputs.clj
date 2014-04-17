(ns searchzy.service.inputs
  (:use [clojure.core.match :only (match)])
  (:require [clojure.string :as str]
            [searchzy.service
             [clean :as clean]
             [geo-locate :as geo-locate]
             [query :as q]]))


;; TODO: Use (Integer. s) ?
(defn str->val
  "Taking an HTTP query parameter and convert it to a proper value,
   using a default value if none provided."
  [str default]
  (if (str/blank? str)
    default
    (or (read-string str) default)))

(defn true-str?
  "Convert string (e.g. from cmd-line or http args) to bool."
  [s]
  (contains? #{"true" "t" "1"} s))

(defn mk-page-map
  "From input info, create usable info."
  [{:keys [from size]}]
  {:from (str->val from 0)
   :size (str->val size 10)})

(defn mk-geo-map
  "Take input-geo-map: miles, address, lat, lon.
   >> Also optional max_miles. <<
   If the input is valid, create a geo-map.
   If not, return nil."
  [{address :address, lat-str :lat, lon-str :lon, miles-str :miles}]
  (let [lat       (str->val lat-str   nil)
        lon       (str->val lon-str   nil)
        miles     (str->val miles-str 4.0)]
    (let [res (geo-locate/resolve-address lat lon address)]
      (if (nil? res)
        nil
        {:address {:input address, :resolved (:address res)}
         :coords (:coords res)
         :miles miles}))))

;; ---

(defn- mk-hours-map
  "At this point, arg should never be blank.
   In other words, we're attempting to filter.
   Return either:
      {:wday, :hour, :minute} -OR-
      nil (error)."
  [non-blank-hours-str]
  (let [strs (clojure.string/split non-blank-hours-str #"[^\d]")]
    (if (or (< (count strs) 3)
            (some clojure.string/blank? strs))
      ;; Error!
      nil
      (try
        (let [[d h m] (map #(Integer. %) strs)]
          ;; TODO: validate:
          ;;   wday:   [0..6]
          ;;   hour:   [0..23]
          ;;   minute: [0..59]
          {:wday d, :hour h, :minute m})
        (catch Exception e
          ;; Error!
          nil)))))

(defn get-hours-map
  "If {}, that's not an error, that just means we don't filter!"
  [hours-str]
  (if (clojure.string/blank? hours-str)
    ;; Not an Error.  Just no filtering.
    {}
    ;; Trying to filter.  May or may not be Error.
    (mk-hours-map hours-str)))

;; ---

(defn get-query
  "blank query is an error"
  [q]
  (let [s (q/normalize q)]
    (if (clojure.string/blank? s) nil s)))

;; i.e. against name of business
(def clean-required-query
  (clean/mk-cleaner
   :query :query
   get-query
   (fn [i o] {:param :query
              :message (str "Param 'query' must be non-empty "
                            "after normalization.")
              :args {:original-query i, :normalized-query o}})))

(def clean-optional-query
  (clean/mk-cleaner
   :query :query
   (fn [q] (if (nil? q)
             ""
             (q/normalize q)))
   (fn [i o] {:param :query
              :message "There should be no problem!"
              :args {:original-query i, :normalized-query o}})))

(def clean-geo-map
  (clean/mk-cleaner
   :geo-map :geo-map
   mk-geo-map
   (fn [i o] {:params [:address :lat :lon]
              :message (str "Must provide: (valid 'address' OR "
                            "('lat' AND 'lon')).")
              :args i})))

(def clean-hours
  (clean/mk-cleaner
   :hours :hours-map
   get-hours-map
   (fn [i o] {:param :hours
              :message "Some problem with the hours."
              :args i})))

(def clean-page-map
  (clean/mk-cleaner
   :page-map :page-map
   mk-page-map
   (fn [i o] {:params [:from :size]
              :message "Some problem with the paging info."
              :args i})))

(def clean-include-unpriced
  (clean/mk-cleaner
   :include-unpriced :include-unpriced
   true-str?     ;; always produces t/f, never error (nil)
   (fn [i o])))  ;; no-op

(def clean-html
  (clean/mk-cleaner
   :html :html
   true-str?    ;; always produces t/f, never error (nil)
   (fn [i o]))) ;; no-op

(defn- get-order-and-attr
  [sort-str]
  (if (clojure.string/blank? sort-str)
    [:desc "value"]
    (let [s (clojure.string/trim sort-str)]
      (if (= \- (first s))
        [:desc, (apply str (rest s))]
        [:asc,  s]))))

(defn- get-sort-map
  "If a non-legal value is supplied, return nil (== error)."
  [sort-str valid-attributes]
  (let [[order attr] (get-order-and-attr sort-str)]
    (if (contains? valid-attributes attr)
      {:attribute attr, :order order}
      nil)))

(defn clean-sort
  [attrs-set]
  (clean/mk-cleaner
   :sort :sort-map
   #(get-sort-map % attrs-set)
   (fn [i o]
     (let [opts (flatten (map (fn [o] [o (str "-" o)]) attrs-set))]
       {:param :sort
        :message "Invalid value for param 'sort'."
        :args i
        :options opts}))))

;; Munge fn for strings -- if blank, return nil.
(defn- str-or-nil
  [s]
  (if (clojure.string/blank? s) nil s))

(def clean-item-id
  (clean/mk-cleaner
   :item-id :item-id
   str-or-nil
   (fn [i o] {:param :item_id  ; underbar
              :message "Param 'item_id' must have non-empty value."})))

(def clean-business-category-ids
  (clean/mk-cleaner
   :business-category-ids :business-category-ids
   (fn [s]
     (if (clojure.string/blank? s)
       []
       (clojure.string/split s #",")))
   (fn [i o] {:param :business_category_ids  ; underbar
              :message "There should be no problem with 'business_category_ids'."})))

(defn get-utc-offset-map
  " '-12'   => {:hour -12, :minute  0}
    '+1'    => {:hour   1, :minute  0}
    '+5:45' => {:hour   5, :minute 45}"
  [offset-str]
  (if (clojure.string/blank? offset-str)
    ;; {} means empty optional.
    {}
    ;; Parse.
    (try
      (let [[_ h _ m]   (re-find #"^\s*([-+]?\d+)(:(\d*))?$" offset-str)
            h-sans-plus (clojure.string/replace-first h "+" "")]
        {:hours   (Integer. h-sans-plus)
         :minutes (if (clojure.string/blank? m) 0 (Integer. m))})
      (catch Exception e nil))))

(def clean-utc-offset
  (clean/mk-cleaner
   :utc-offset :utc-offset-map
   get-utc-offset-map
   (fn [i o] {:param :utc-offset
              :message "'utc_offset' should have values like '-5' or '+5:45'."
              :args i})))

;;-----------------------

(defn business-clean-input
  "Validate each argument group in turn.
   Gather up any validation errors as you go."
  [args]
  (let [sort-attrs #{"value" "distance" "score"}]
    (clean/gather->> args
                     ;; One is required, either query or biz-cat-ids.
                     clean-optional-query
                     clean-business-category-ids
                     ;;
                     clean-geo-map
                     clean-hours
                     clean-utc-offset
                     (clean-sort sort-attrs)
                     clean-page-map)))

(defn biz-menu-item-clean-input
  "Validate each argument group in turn.
   Gather up any validation errors as you go."
  [args]
  (let [sort-attrs #{"price" "value" "distance"}]
    (clean/gather->> args
                     clean-item-id
                     clean-include-unpriced
                     clean-geo-map
                     clean-hours
                     clean-utc-offset
                     (clean-sort sort-attrs)
                     clean-page-map)))

;; v1
(defn suggestion-clean-input-v1
  "Validate each argument group in turn.
   Gather up any validation errors as you go."
  [args]
  (clean/gather->> args
                   clean-required-query
                   clean-business-category-ids
                   clean-html
                   clean-geo-map
                   clean-page-map))

;; v2 - blank query okay
(defn suggestion-clean-input-v2
  "Validate each argument group in turn.
   Gather up any validation errors as you go."
  [args]
  (clean/gather->> args
                   clean-optional-query
                   clean-business-category-ids
                   clean-html
                   clean-geo-map
                   clean-utc-offset
                   clean-page-map))
