(ns searchzy.cfg)

;;
;; Cluster name and transport node addresses can be retrieved
;; via HTTP API, for example:
;;
;;   > curl http://localhost:9200/_cluster/nodes
;;   {"ok":true,"cluster_name":"elasticsearch_markwong-vanharen","nodes":...}}
;;
(def elastic-search-cfg
  {:cluster-name "elasticsearch_markwong-vanharen"
   :host "127.0.0.1"
   :port 9300})

(def mongo-db-cfg
  {:db-name "centzy_web_production"
   :host "127.0.0.1"
   :port 27017})

;; If you'd like to use different ElasticSearch indices, just change these.
;; You'll have to re-index before you can search against them, obviously.
(def elastic-search-names
  {:businesses           {:index "businesses"
                          :mapping "business"}
   :business_categories  {:index "business_categories"
                          :mapping "business_category"}
   :items                {:index "items"
                          :mapping "item"}
   :business_menu_items  {:index "business_menu_items"
                          :mapping "business_menu_item"}})
