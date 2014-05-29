(ns searchzy.service.geo-locate
  (:use [clojure.core.match :only (match)])
  (:require [clojure.string :as str]
            [clojure.core.cache :as cache]
            [searchzy.cfg :as cfg]
            [geocoder
             [google :as goog]
             [bing :as bing]]))     ;; #{bing geonames google}

(defn- mk-bing-geolocate
  "Returns a function, based on whether an api-key is provided or not.
   The returned function...
   Given an address (e.g. '2491 Aztec Way, Palo Alto, CA 94303'),
   return: {:address (resolved), :coords} || nil"
  [api-key]
  (if (str/blank? api-key)
    (do
      (println "Cannot use 'bing' for geocoding because 'bing-api-key' in config is empty.")
      (fn [addr] nil))  ;; no-op
    (fn [address]
      (if (str/blank? address)
        nil
        (try
          (if-let [result (first (bing/geocode-address address :api-key api-key))]
            (let [[lat lon] (-> result :point :coordinates)]
              {:coords {:lat lat, :lon lon}
               :address (:name result)})
            nil)
          ;; TODO: log something here
          (catch Exception e
            (do
              (println "Bing geolocate exception:" (str e))
              nil)))))))

(defn- goog-geolocate
  "Given an address (e.g. '2491 Aztec Way, Palo Alto, CA 94303'),
   return: {:address (resolved), :coords} || nil"
  [address]
  ;; -- Makes remote call. --
  ;; Google uses 'lng' instead of 'lon'.
  ;; NB: We change it: :lng -> :lon.
  (if (str/blank? address)
    nil
    (try
      (if-let [result (first (goog/geocode-address address))]
        (let [resolved-address (:formatted-address result)
              {:keys [lat lng]} (:location (:geometry result))]
          (if (nil? resolved-address)
            nil
            {:coords {:lat lat, :lon lng}
             :address resolved-address}))
        nil)
      ;; TODO: log something here
      (catch Exception e
        (do 
          (println "Google geolocate exception:" (str e))
          nil)))))

(def default-provider "google")

(defn- get-provider
  "Gets geocoding provider from config.
   If fails, returns default-provider."
  [config]
  (try
    ;; clojure.string functions may raise NullPointerException.
    (-> config :geocoding :provider str/trim str/lower-case)
    (catch Exception e
      (do (println "WARNING: Problem reading geocoding CFG:" (str e))
          default-provider))))

;; Function reference.  Which implementation depends on config.
(def geolocate
  (let [config (cfg/get-cfg)
        provider (get-provider config)
        bing-geolocate (mk-bing-geolocate (-> config :geocoding :bing-api-key))]
    (match provider
           ;; First try one.  Upon failure, try the other.
           "google" (fn [addr] (or (goog-geolocate addr) (bing-geolocate addr)))
           "bing"   (fn [addr] (or (bing-geolocate addr) (goog-geolocate addr)))
           :else    (throw (Exception.
                            (str "Invalid geocoding provider in CFG: "
                                 provider))))))

(def CACHE (atom (cache/lru-cache-factory {} :threshold (Math/pow 2 16))))

;; FIXME: Is it a good idea to remove all punctuation?
;; What about street addresses like this: "12-145 Haiku Plantation"?
(defn- canonicalize-address
  "Lowercase.  Remove punctuation.  Scrunch whitespace."
  [s]
  (assert s)
  (str/join " " (-> s
                    str/lower-case
                    ;; replace commas with spaces
                    (str/replace #"," " ")
                    ;; remove all other punctuation
                    (str/replace #"\p{P}" "")
                    ;; scrunch whitespace
                    str/trim
                    (str/split #"\s+"))))

;;-----------------------

(defn- resolve-addr-str
  [addr]
  (if-let [loc (geolocate addr)]
    (:address loc)
    nil))

(defn- resolve-and-canonicalize-addr
  "[k v] -> [k v]"
  [[cfg-addr cfg-coords]]
  (let [cfg-addr-str (name cfg-addr)
        resolved (resolve-addr-str cfg-addr-str)
        addr (or resolved cfg-addr-str)]
    [(canonicalize-address addr) cfg-coords]))

;; Also defined in clojure.walk,
;; but that one is recursive (and we don't need that).
(defn stringify-keys
  "stringifies only top-level keys"
  [map]
  (let [f (fn [acc [k v]] (assoc acc (name k) v))]
    (reduce f {} map)))

;;
;; We want to be able to find an address either:
;;    * as provided in the configuration (but canonicalized!), OR
;;    * as resolved by bing/google (but canonicalized!)
;;
;; So we create a hash which uses both versions of the address
;; as its keys.
;;
(defn- mk-resolved-addr-2-coords
  [cfg-map]
  (let [locs (map resolve-and-canonicalize-addr cfg-map)]
    (apply hash-map (flatten locs))))

(defn- mk-preferred-coords []
  (let [cfg-map (-> (cfg/get-cfg) :geocoding :preferred-coords)]
    (if (empty? cfg-map)
      ;; If nothing in config...
      {}
      ;; else use config...
      (merge (stringify-keys cfg-map)
             (mk-resolved-addr-2-coords cfg-map)))))

(def preferred-coords)
(defn get-preferred-coords []
  (defonce preferred-coords (mk-preferred-coords))
  preferred-coords)

;;-----------------------

(defn- from-config
  [addr]
  (let [a-2-cs (get-preferred-coords)]
    (if-let [coords (a-2-cs addr)]
      {:address addr, :coords coords}
      nil)))

(defn- from-cache
  "If get from cache, notify cache that we've used it and return value.
   If failed, return nil."
  [addr]
  (if-let [loc (cache/lookup @CACHE addr)]
    (do (swap! CACHE #(cache/hit % addr))
        loc)
    nil))

(defn- lookup
  "Perform geo lookup.  Success: add to cache.  Failure: return nil."
  [addr]
  ;; If bing/google lookup works...
  (if-let [lookup-loc (geolocate addr)]

    ;; See whether the "resolved" address is in the config.
    (let [canon-lookup-addr (canonicalize-address (:address lookup-loc))]

      ;; If it is in config, opt for the config loc.
      ;; Else, opt for the looked-up one.
      (let [loc (or (from-config canon-lookup-addr)
                    lookup-loc)]
        ;; Cache the resolved address with the loc, and
        ;; return the proper loc.
        (swap! CACHE #(cache/miss % canon-lookup-addr loc))
        (swap! CACHE #(cache/miss % addr loc))
        loc))

    ;; If bing/google lookup fails, we lose.
    nil))

(defn resolve-address
  "If lat,lon are good, just return those coords with no resolved address.
   Otherwise use address to look up geocoordinates.
   Returns: {:address (resolved), :coords} || nil"
  [lat lon address]
  ;; if both not nil
  (if-not (or (nil? lat) (nil? lon))
    ;; No need to lookup.
    {:coords {:lat lat, :lon lon}
     :address nil}
    ;; Try cache, then lookup if necessary.
    (if (nil? address)
      nil
      (let [a (canonicalize-address address)]
        (or (from-config a)
            (from-cache a)
            (lookup a))))))
