(ns searchzy.service.geo
  (:require [geocoder.google :as geo]))     ;; #{bing geonames google}
            
(defn -get-geolocation
  "Given an address (e.g. '2491 Aztec Way, Palo Alto, CA 94303'),
   return a map with keys :lat, :lng."
  [address]
  ;; -- Makes remote call. --
  (:location (:geometry (first (geo/geocode-address address)))))

(defn get-lat-lng
  "If lat,lng are good, just return as map.
   Otherwise use address to look up geocoordinates (and return as map)."
  [lat lng address]
  (if (or (nil? lat) (nil? lng))
    (-get-geolocation address)
    {:lat lat :lng lng}))

