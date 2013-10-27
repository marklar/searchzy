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

(defn -main [& args]

  ;; elastic search
  (util/es-connect! cfg/elastic-search-cfg)

  (let [cfg  cfg/elastic-search-cfg
        res  (es-doc/search "businesses" "business"
                            :query {:match {:name "Palo"}})
        n    (es-rsp/total-hits res)
        hits (es-rsp/hits-from res)]
    
    (println (format "Total hits: %d" n))
    (pp/pprint res)))

