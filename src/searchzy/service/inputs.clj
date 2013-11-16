(ns searchzy.service.inputs
  (:require [clojure.string :as str]
            [searchzy.service
             [flurbl :as flurbl]
             [geo :as geo]
             [query :as q]]))


;; TODO: Use (Integer. s) ?
(defn str-to-val
  "Taking an HTTP query parameter and convert it to a proper value,
   using a default value if none provided."
  [str default]
  (if (str/blank? str)
    default
    (or (read-string str) default)))

(defn true-str?
  "Convert string (e.g. from cmd-line or http params) to bool."
  [s]
  (contains? #{"true" "t" "1"} s))

(defn mk-page-map
  "From input info, create usable info."
  [{:keys [from size]}]
  {:from (str-to-val from 0)
   :size (str-to-val size 10)})

(defn mk-geo-map
  "Take input-geo-map: miles, address, lat, lon.
   If the input is valid, create a geo-map.
   If not, return nil."
  [{address :address, lat-str :lat, lon-str :lon, miles-str :miles}]
  (let [lat   (str-to-val lat-str   nil)
        lon   (str-to-val lon-str   nil)
        miles (str-to-val miles-str 4.0)]
    (let [res (geo/resolve-address lat lon address)]
      (if (nil? res)
        nil
        {:address {:input address, :resolved (:address res)}
         :coords (:coords res)
         :miles miles}))))

;; ---

(defn- get-day-hour-minute
  [input-map]
  (let [hours-str (:hours input-map)]
    (if (not (clojure.string/blank? hours-str))
      ;; from combo-string
      (clojure.string/split hours-str #"[^\d]")
      ;; from separate strings
      (let [{:keys [wday hour minute]} input-map]
        [wday hour minute]))))

(defn get-hours-map
  ;; FIXME
  "If {}, that's not an error, that just means we don't filter!"
  [input-map]
  (let [strs (get-day-hour-minute input-map)]
    (if (not (and (= 3 (count strs))
                  (not-any? clojure.string/blank? strs)))
      {}
      ;; TODO:
      ;; try / catch, in case (Integer.) doesn't work.
      ;; validate:
      ;;   wday:   [0..6]
      ;;   hour:   [0..23]
      ;;   minute: [0..59]
      {:wday   (Integer. (get strs 0))
       :hour   (Integer. (get strs 1))
       :minute (Integer. (get strs 2))
       })))


;; ---

;;
;; Validation.
;; Pass all the args which need validation through a series of validation fns.
;; Validate each in turn, passing state as you go.
;; If you encounter a problem, either:
;;   -- continue, adding it to the state's aggregate collection of problems.
;;   -> short-circuit, returning the problem in the state.
;; When done, check the state.  If a problem, return it.  If not, continue.
;; 

(defn mk-cleaner
  "Higher-order fn which creates 'clean-' fns.
   Params:
     - oldput-key : name (Keyword) of the input arg group we wish to 'clean'
     - input-key  : name (Keyword) of the arg group to use in output
     - munge-fn   : fn :: input -> desired-output (of the arg group)
                    Upon validation error, must return nil.
     - error-fn   : fn :: (input, output) -> error-hashmap

   The created fn takes the hashmap of all args,
   attempts to 'munge' the desired subset,
   and returns either:
       [value nil] - upon success
       [nil error] - upon failure
  "
  [input-key output-key munge-fn error-fn]
  (fn [args]
    (let [i (get args input-key)
          o (munge-fn i)]
      (if (nil? o)
        [nil
         (error-fn i o)]
        [(-> args (dissoc input-key) (assoc output-key o))
         nil]))))

(def clean-query
  (mk-cleaner :query :query
              (fn [i]
                (let [o (q/normalize i)]
                  (if (clojure.string/blank? o) nil o)))
              (fn [i o] {:error (str "Param 'query' must be non-empty "
                                     "after normalization.")
                         :params {:original-query i
                                  :normalized-query o}})))

(def clean-geo-map
  (mk-cleaner :geo-map :geo-map
              mk-geo-map
              (fn [i o] {:error (str "Must provide: (valid 'address' OR "
                                     "('lat' AND 'lon')).")
                         :params i})))

(def clean-hours-map
  (mk-cleaner :hours-map :hours-map
              get-hours-map
              (fn [i o] {:error "Some problem with the hours."
                         :params i})))

(def clean-page-map
  (mk-cleaner :page-map :page-map
              mk-page-map
              (fn [i o] {:error "Some problem with the paging info."
                         :params i})))

(def clean-html
  (mk-cleaner :html :html
              true-str?        ;; always produces t/f, never error (nil)
              (fn [i o] nil))) ;; no-op

(defn mk-sort-cleaner
  [attrs-set]
  (mk-cleaner :sort :sort-map
              #(flurbl/get-sort-map % attrs-set)
              (fn [i o] {:error "Invalid value for 'sort'."
                         :params i})))

(defn bind-err
  [f [val err]]
  (if (nil? err)
    (f val)
    [nil err]))
  
(defmacro err->>
  "For syntactic convenience.
   Macro fns take their arguments _un_evaluated.
  "
  [val & fns]
  (let [fns (for [f fns] `(bind-err ~f))]
    `(->> [~val nil]
          ~@fns)))

(defn business-clean-input
  "Validate each argument group in turn.
   If find validation error, short-circuit and return the error."
  [args sort-attrs]
  (err->> args
          clean-query
          clean-geo-map
          clean-hours-map
          (mk-sort-cleaner sort-attrs)
          clean-page-map))


;; clean-item-id
;; (if (clojure.string/blank? item-id)
;;   (responses/error-json {:error "Param 'item_id' must be non-empty."})

(defn biz-menu-item-clean-input
  "Validate each argument group in turn.
   If find validation error, short-circuit and return the error."
  [args sort-attrs]
  (err->> args
          ;; TODO: add clean-item-id
          ;; clean-item-id
          clean-geo-map
          clean-hours-map
          (mk-sort-cleaner sort-attrs)
          clean-page-map))

(defn suggestion-clean-input
  [args]
  (err->> args
          clean-query
          clean-html
          clean-geo-map
          clean-page-map))
          
