(ns searchzy.index.location
  (:use [searchzy.util])
  (:require [searchzy.index.util :as util]
            [searchzy.cfg :as cfg]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native.document :as es-doc]))

;; Get CFG info.
(def idx-name     (:index   (:locations cfg/elastic-search-names)))
(def mapping-name (:mapping (:locations cfg/elastic-search-names)))

;; Make this just nil?
(def mapping-types
  {mapping-name
   {:properties
    {:_id {:type "string"}
     :permalink {:type "string"}}}})

(defn- mg->es
  [mg-map]
  (-> mg-map
      (dissoc :_id)))

(defn- add-to-idx
  [mg-map]
  (es-doc/put idx-name mapping-name
              (str (:_id mg-map))
              (mg->es mg-map)))

(defn- recreate-idx
  []
  (util/recreate-idx idx-name mapping-types))

(defn mk-idx
  [& {:keys [limit]}]
  (recreate-idx)
  (doseq-cnt add-to-idx
             1000
             (mg/fetch :locations :limit limit)))
