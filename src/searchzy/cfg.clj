(ns searchzy.cfg)

(def elastic-search-cfg
  {:cluster-name "elasticsearch_markwong-vanharen"
   :host "127.0.0.1"
   :port 9300})

(def mongo-db-cfg
  {:db-name "centzy2_development"
   :host "127.0.0.1"
   :port 27017})
