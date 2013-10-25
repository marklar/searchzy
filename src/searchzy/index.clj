(ns searchzy.index
  (:require [clojurewerkz.elastisch.native :as es]
            [clojurewerkz.elastisch.native.index :as es-idx]
            [clojurewerkz.elastisch.native.document :as es-doc]))

(defn es-connect
  "ElasticSearch native client connection."
  [cluster-name]
  (es/connect! [["127.0.0.1", 9300]]
               {"cluster.name" cluster-name}))

(defn recreate-idx
  [idx-name mapping-types]
  (es-idx/delete idx-name)
  (if (not (es-idx/exists? idx-name))
    (es-idx/create idx-name :mappings mapping-types)))

;; CONNECT
(def cluster-name "elasticsearch_markwong-vanharen")
(es-connect cluster-name)
