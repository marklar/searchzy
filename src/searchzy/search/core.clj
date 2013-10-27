(ns searchzy.search.core
  (:require [searchzy
             [cfg  :as cfg]
             [util :as util]]
            [clojurewerkz.elastisch
             [native :as es]
             [query  :as es-q]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]
             [response :as es-rsp]]
            [clojure.pprint :as pp]))

(defn -main [query & args]

  (util/es-connect! cfg/elastic-search-cfg)

  (let [res  (es-doc/search "businesses" "business"
                            :query {:match {:name query}})
        n    (es-rsp/total-hits res)
        hits (es-rsp/hits-from res)]
    
    (println (format "Total hits: %d" n))
    (pp/pprint res)))

