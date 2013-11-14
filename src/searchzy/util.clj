(ns searchzy.util
  (:require [clojure.string :as str]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native :as es]))

(defn doseq-cnt
  "Call function 'f' on each of 'seq',
   printing count out after each 'num' items."
  [f num seq]
  (let [cnt (atom 0)]
    (doseq [i seq]
      (f i)
      (swap! cnt inc)
      (if (= 0 (mod @cnt num))
        (println @cnt)))
    @cnt))

;; TODO: Move these to "connect" module?

;; -- elastic-search --

(defn es-connect!
  "ElasticSearch native client connection."
  [es-cfg]
  (es/connect! [[(:host es-cfg), (:port es-cfg)]]
               {"cluster.name" (:cluster-name es-cfg)}))

;; -- mongo --

(defn- auth-str
  [u p]
  ;; (if u
  ;;   (str u
  ;;        ":"
  ;;        (if (str/blank? p) "" (str ":" p))
  ;;        "@")
  ;;   ""))
  (if (and u p)
    (str u ":" p "@")
    ""))

(defn mk-conn-str
  [db-name {:keys [host port username password]}]
  (str "mongodb://"
       (auth-str username password)
       host ":" port "/" db-name))
  
(defn mongo-connect!
  "MongoDB connection."
  [db-name mg-cfg]
  (println "conn-str: " (mk-conn-str db-name mg-cfg))
  (let [conn (mg/make-connection (mk-conn-str db-name mg-cfg))]
    (mg/set-connection! conn)))
