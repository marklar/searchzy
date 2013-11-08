(ns searchzy.cfg
  (:use [clojure.java.io])
  (:require [clj-yaml.core :as yaml]))

;; -- default config --


;; If you'd like to use different ElasticSearch indices, just change these.
;; You'll have to re-index before you can search against them, obviously.
(def elastic-search-names
  {:businesses           {:index   "businesses"
                          :mapping "business"}
   :business_categories  {:index   "business_categories"
                          :mapping "business_category"}
   :items                {:index   "items"
                          :mapping "item"}
   :business_menu_items  {:index   "business_menu_items"
                          :mapping "business_menu_item"}})

;;
;; Cluster name and transport node addresses can be retrieved
;; via HTTP API, for example:
;;
;;   > curl http://localhost:9200/_cluster/nodes
;;   {"ok":true,"cluster_name":"elasticsearch_markwong-vanharen","nodes":...}}
;;

(def default-cfg
  {:elastic-search {:cluster-name "elasticsearch_markwong-vanharen"
                    :host "127.0.0.1"
                    :port 9300}
   :mongo-db       {:db-name "centzy_web_production"
                    :host "127.0.0.1"
                    :port 27017}})

(def cfg (atom nil))

;; -- yaml config --

;; FIXME -- Currently, it just looks in whatever the *current* directory is.
(def cfg-file-name ".config.yaml")

(defn create-yaml-str []
  (yaml/generate-string default-cfg))

(defn dump-cfg [file-name]
  (spit file-name (create-yaml-str)))

(defn -load-cfg
  "If cfg file exists, return cfg data.
   If it does not, return default-cfg."
  [file-name]
  (if (.exists (as-file file-name))
    (do
      (println "Reading in configuration from file: " file-name)
      (let [yaml-str (slurp file-name)]
        (yaml/parse-string yaml-str)))
    (do
      (println "No configuration file found.  Using default config settings.")
      default-cfg)))

(defn get-cfg []
  (if (nil? @cfg)
    (reset! cfg (-load-cfg cfg-file-name)))
  @cfg)
