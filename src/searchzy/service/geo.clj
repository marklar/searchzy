(ns searchzy.service.geo
  (:require [geocoder.google :as geo]))     ;; #{bing geonames google}
            
(defn get-geolocation
  "Given an address (e.g. '2491 Aztec Way, Palo Alto, CA 94303'),
   return a map with keys :lat, :lon."
  [address]
  ;; -- Makes remote call. --
  ;; Google uses 'lng' instead of 'lon'.  (Which is better?)
  ;; NOTE: We're changing the key from 'lng' to 'lon'!!!
  (clojure.set/rename-keys 
   (:location (:geometry (first (geo/geocode-address address))))
   {:lng :lon}))

(defn get-lat-lon
  "If lat,lon are good, just return as map.
   Otherwise use address to look up geocoordinates (and return as map)."
  [lat lon address]
  (if (or (nil? lat) (nil? lon))
    (get-geolocation address)
    {:lat lat :lon lon}))

