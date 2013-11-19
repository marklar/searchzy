(ns searchzy.service.geo
  (:use [clojure.core.match :only (match)])
  (:require [clojure.string :as str]
            [clojure.core.cache :as cache]
            [searchzy.cfg :as cfg]
            [geocoder
             [google :as goog]
             [bing :as bing]]))     ;; #{bing geonames google}

(defn- bing-geolocate
  "Given an address (e.g. '2491 Aztec Way, Palo Alto, CA 94303'),
   return: {:address (resolved), :coords} || nil"
  [api-key]
  (if (str/blank? api-key)
    (throw (Exception. (str "Using 'bing' for geocoding, but "
                            "'bing-api-key' in config is empty.")))
    (fn [address]
      (if (str/blank? address)
        nil
        (try
          (let [result (first (bing/geocode-address address :api-key api-key))]
            (if (nil? result)
              nil
              (let [[lat lon] (-> result :point :coordinates)]
                {:coords {:lat lat, :lon lon}
                 :address (:name result)})))
          ;; TODO: log something here
          (catch Exception e (do (println (str e))
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
      (let [result (first (goog/geocode-address address))
            resolved-address (:formatted-address result)
            {:keys [lat lng]} (:location (:geometry result))]
        (if (nil? resolved-address)
          nil
          {:coords {:lat lat, :lon lng}
           :address resolved-address}))
      ;; TODO: log something here
      (catch Exception e (do (println (str e))
                             nil)))))

(defn- get-provider
  "Gets geocoding provider from config.
   If fails, returns 'google' by default."
  [config]
  (try
    ;; clojure.string functions may raise null pointer exception.
    (-> config :geocoding :provider str/trim str/lower-case)
    (catch Exception e (do (println (str e))
                           "google"))))

;; Function.  Which implementation depends on config.
(def geolocate
  (let [config (cfg/get-cfg)
        provider (get-provider config)]
    (match provider
           "google" goog-geolocate
           "bing"   (bing-geolocate (-> config :geocoding :bing-api-key))
           :else    (throw (Exception.
                            (str "Invalid geocoding provider in CFG: "
                                 provider))))))

(def C (atom (cache/lru-cache-factory {} :threshold (Math/pow 2 16))))

(defn- canonicalize-address
  "Lowercase.  Remove punctuation.  Scrunch whitespace."
  [s]
  (str/join " " (-> s
                    str/lower-case
                    ;; replace commas with spaces
                    (str/replace #"," " ")
                    ;; remove all other punctuation
                    (str/replace #"\p{P}" "")
                    ;; scrunch whitespace
                    str/trim
                    (str/split #"\s+"))))

(defn- from-cache
  "If get from cache, notify cache that we've used it and return value.
   If failed, return nil."
  [addr]
  (if-let [loc (cache/lookup @C addr)]
    (do (swap! C #(cache/hit % addr))
        loc)
    nil))

(defn- lookup
  "Perform geo lookup.  Success: add to cache.  Failure: return nil."
  [addr]
  (if-let [loc (get-geolocation addr)]
    (do (swap! C #(cache/miss % addr loc))
        loc)
    nil))
    
(defn resolve-address
  "If lat,lon are good, just return those coords with no resolved address.
   Otherwise use address to look up geocoordinates.
   Returns: {:address (resolved), :coords} || nil"
  [lat lon address]
  (if (not (or (nil? lat) (nil? lon)))
    ;; No need to lookup.
    {:coords {:lat lat, :lon lon}
     :address nil}
    ;; Try cache, then lookup if necessary.
    (let [a (canonicalize-address address)]
      (or (from-cache a)
          (lookup a)))))
