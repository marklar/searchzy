(ns searchzy.service.geo
  (:require [geocoder.google :as geo]))     ;; #{bing geonames google}

(defn get-geolocation
  "Given an address (e.g. '2491 Aztec Way, Palo Alto, CA 94303'),
   return:
     - success: a map with keys :lat, :lon
     - failure: nil
  "
  [address]
  ;; -- Makes remote call. --
  ;; Google uses 'lng' instead of 'lon'.
  ;; NB: We change it: :lng -> :lon.
  (try
    (let [{:keys [lat lng]}
              (:location (:geometry (first (geo/geocode-address address))))]
      {:lat lat, :lon lng})
    nil))
    
(defn get-lat-lon
  "If lat,lon are good, just return as map.
   Otherwise use address to look up geocoordinates (and return as map)."
  [lat lon address]
  (if (or (nil? lat) (nil? lon))
    (get-geolocation address)
    {:lat lat :lon lon}))
