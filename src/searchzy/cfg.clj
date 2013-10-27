(ns searchzy.cfg)

;;
;; Cluster name and transport node addresses can be retrieved
;; via HTTP API, for example:
;;
;;   > curl http://localhost:9200/_cluster/nodes
;;   {"ok":true,"cluster_name":"elasticsearch_antares","nodes":...}}
;;
(def elastic-search-cfg
  {:cluster-name "elasticsearch_markwong-vanharen"
   :host "127.0.0.1"
   :port 9300})

(def mongo-db-cfg
  {:db-name "centzy2_development"
   :host "127.0.0.1"
   :port 27017})
