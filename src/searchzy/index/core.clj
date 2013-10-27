(ns searchzy.index.core
  (:require [searchzy
             [util :as util]
             [cfg :as cfg]]
            [searchzy.index
             [business :as biz]
             [item-category :as item-cat]
             [business-category :as biz-cat]]))

;; MAIN
(defn -main [& args]

  ;; elastic search
  (util/es-connect! cfg/elastic-search-cfg)
  
  ;; mongo
  (util/mongo-connect! cfg/mongo-db-cfg)
  
  (doseq [[name f] {:BusinessCategories biz-cat/mk-idx
                    :ItemCategories     item-cat/mk-idx
                    :Businesses         biz/mk-idx}]
    (println (str "Indexing " name "..."))
    (let [cnt (f)]
      (println (str "Indexed " cnt " " name " records.")))))
