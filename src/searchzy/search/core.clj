(ns searchzy.search.core
  (:require [searchzy
             [cfg  :as cfg]
             [util :as util]]
            [searchzy.search
             [util :as s-util]
             [business :as biz]]
            [clojurewerkz.elastisch.native [response :as es-rsp]]
            [clojure.pprint :as pp]))

(defn -main [query miles address lat lng by-value from size & args]

  (util/es-connect! cfg/elastic-search-cfg)

  (let [;; transform cmd-line inputs
        miles      (s-util/str-to-val miles 4.0)
        lat        (s-util/str-to-val lat nil)
        lng        (s-util/str-to-val lng nil)
        by-value?  (s-util/true-str? by-value)
        from       (s-util/str-to-val from 0)
        size       (s-util/str-to-val size 10)
        ;; look stuff up
        res  (biz/search query address miles lat lng by-value? from size)
        ;; extract results info
        n     (es-rsp/total-hits res)
        hits  (es-rsp/hits-from res)]
    
    (println (str "Query: " query ". "
                  "Distance: " miles "m. "
                  "Address: '" address "'. "
                  "Lat,lng: " lat "," lng ". "
                  "By-value?: " by-value? ". "
                  "From,size: " from "," size "."))

    (println (format "Total hits: %d" n))
    (pp/pprint res)))

