(ns searchzy.search.core
  (:require [searchzy
             [cfg  :as cfg]
             [util :as util]]
            [searchzy.search
             [util :as s-util]
             [business :as biz]]
            [clojurewerkz.elastisch.native [response :as es-rsp]]
            [clojure.pprint :as pp]))


(defn -main [query miles lat lng by-value? from size & args]

  (util/es-connect! cfg/elastic-search-cfg)

  (println (str "Query: " query ". "
                "Distance: " miles "m. "
                "Lat,lng: " lat "," lng ". "
                "By-value?: " by-value? ". "
                "From,size: " from "," size "."))

  (let [res  (biz/search query
                         miles lat lng
                         (s-util/true-str? by-value?)
                         (read-string from)
                         (read-string size))
        n    (es-rsp/total-hits res)
        hits (es-rsp/hits-from res)]
    
    (println (format "Total hits: %d" n))
    (pp/pprint res)))

