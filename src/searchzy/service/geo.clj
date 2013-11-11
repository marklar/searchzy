(ns searchzy.service.geo
  (:require [geocoder.google :as geo]))     ;; #{bing geonames google}

(defn get-geolocation
  "Given an address (e.g. '2491 Aztec Way, Palo Alto, CA 94303'),
   return: {:address (resolved), :coords} || nil"
  [address]
  ;; -- Makes remote call. --
  ;; Google uses 'lng' instead of 'lon'.
  ;; NB: We change it: :lng -> :lon.
  (if (clojure.string/blank? address)
    nil
    (try (let [result (first (geo/geocode-address address))
               resolved-address (:formatted-address result)
               {:keys [lat lng]} (:location (:geometry result))]
           {:coords {:lat lat, :lon lng}
            :address resolved-address})
         (catch Exception e nil)
         (finally nil))))
    
(defn resolve-address
  "If lat,lon are good, just return those coords with no resolved address.
   Otherwise use address to look up geocoordinates.
   Returns: {:address (resolved), :coords} || nil"
  [lat lon address]
  (if (or (nil? lat) (nil? lon))
    (get-geolocation address)
    {:coords {:lat lat, :lon lon}
     :address nil}))
