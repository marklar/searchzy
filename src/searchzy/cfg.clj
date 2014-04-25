(ns searchzy.cfg
  (:use [clojure.java.io])
  (:require [clj-yaml.core :as yaml]))

;; -- default config --

;; FIXME: This bit of config strikes me as overly complex.
;; When does one really want to change which ES indices one uses?
;;
;; If you'd like to use different ElasticSearch indices, just change these.
;; You'll have to re-index before you can search against them, obviously.
(def elastic-search-names
  {
   ;; -- slow --
   :businesses           {:index   "businesses"
                          :mapping "business"}
   :business_menu_items  {:index   "business_menu_items"
                          :mapping "business_menu_item"}
   :lists                {:index   "lists"
                          :mapping "list"}
   ;; -- fast --
   :business_categories  {:index   "business_categories"
                          :mapping "business_category"}
   :items                {:index   "items"
                          :mapping "item"}
   })

;;
;; Cluster name and transport node addresses can be retrieved
;; via HTTP API, for example:
;;
;;   > curl -XGET 'http://localhost:9200/_nodes/cluster_name'
;;   {"cluster_name":"elasticsearch","nodes":{}}
;;

(def default-cfg
  {:api-key nil

   :geocoding {:provider "bing"  ;; google
               :bing-api-key nil}

   :elastic-search {:cluster-name "elasticsearch"
                    :host "127.0.0.1"
                    :port 9300}

   :mongo-db
   {
    :main
    {:db-name "centzy_web_production"
     :username nil
     :password nil
     :host "127.0.0.1"
     :port 27017}

    :areas 
    {:db-name "locality_web_production_areas"
     :username "locality_web_05_2014"
     :password "JvJ6XlTIE0nf9mnggPTu7EImHfH7RsuwekgD24F152WaSip85da"
     :host "locality-web-production-areas.m0.mongolayer.com"
     :port 27017}
    
    ;; :areas
    ;; {:db-name "centzy_web_production_areas"
    ;;  :username nil
    ;;  :password nil
    ;;  :host "127.0.0.1"
    ;;  :port 27017}
    
    :businesses
    {:db-name "centzy_web_production"
     :username nil
     :password nil
     :host "127.0.0.1"
     :port 27017}}})

;; -- yaml config --

;; FIXME -- Currently, it just looks in whatever the *current* directory is.
(def cfg-file-name ".config.yaml")

(defn- create-yaml-str []
  (yaml/generate-string default-cfg))

(defn- dump-cfg [file-name]
  (spit file-name (create-yaml-str)))

(defn load-cfg
  "If cfg file exists, return cfg data.
   If it does not, return default-cfg."
  [file-name]
  (if (.exists (as-file file-name))
    (do
      (println "Reading in configuration from file:" file-name)
      (let [yaml-str (slurp file-name)]
        (yaml/parse-string yaml-str)))
    (do
      (println "No configuration file found.  Using default config settings.")
      default-cfg)))

(def cfg)
(defn get-cfg []
  (defonce cfg (load-cfg cfg-file-name))
  cfg)
