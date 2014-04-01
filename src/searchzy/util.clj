(ns searchzy.util
  (:require [clojure.string :as str]
            [searchzy.cfg :as cfg]
            [somnium.congomongo :as mg]
            [clojurewerkz.elastisch.native :as es]))

(defn maybe-take
  [limit seq]
  (if (nil? limit)
    seq
    (take limit seq)))

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
  (if (and u p)
    (str u ":" p "@")
    ""))

(defn mk-conn-str
  [{:keys [db-name host port username password]}]
  (str "mongodb://"
       (auth-str username password)
       host ":" port "/" db-name))
  
(defn mongo-connect!
  "Sets 'current' MongoDB connection.
   i.e. Changes the state of the world.
   TODO: Do this more functionally?"
  [mg-cfg]
  (println "conn-str: " (mk-conn-str mg-cfg))
  (let [conn (mg/make-connection (mk-conn-str mg-cfg))]
    (mg/set-connection! conn)))

(defn mongo-connect-db!
  "Given a MongoDB collection name,
   attempt to establish connection to it."
  [db-name]
  ;; This just assumes that the db-name will be correct.
  ;; FIXME: add check.
  (let [cfc (get (:mongo-db (cfg/get-cfg)) db-name)]
    (mongo-connect! cfc)))
