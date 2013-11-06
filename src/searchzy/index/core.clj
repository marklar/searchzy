(ns searchzy.index.core
  (:require [searchzy
             [util :as util]
             [cfg :as cfg]]
            [clojurewerkz.elastisch.native.index :as es-idx]
            [searchzy.index
             [business :as biz]
             [item :as item]
             [business-menu-item :as biz-menu-item]
             [business-category :as biz-cat]]))

(defn -rm-index [name]
  (let [pre (str "index '" name "': ")]
    (if (es-idx/exists? name)
      (do (println (str pre "exists.  deleting."))
          (es-idx/delete name))
      (println (str pre "doesn't exist.")))))

(defn -rm-es-indices [names]
  (doseq [n names]
    (-rm-index n)))

(defn -index-all []
  "Serial index creation.
   Use either this or -par-index-all (parallel)."
  (doseq [[name f] {:BusinessMenuItems  biz-menu-item/mk-idx
                    :BusinessCategories biz-cat/mk-idx
                    :Items              item/mk-idx
                    :Businesses         biz/mk-idx}]
    (println (str "Indexing " name "..."))
    (let [cnt (f)]
      (println (str "Indexed " cnt " " (str name) " records.")))))

(defn -update
  [[name f]]
  (println (str "indexing: " name))
  (let [cnt (f)]
    (println (str "indexed " cnt " " (str name) " records."))
    cnt))

(defn -par-index-all []
  (let [agents (doall (map agent {"Biz Menu Items" biz-menu-item/mk-idx
                                  "Biz Categories" biz-cat/mk-idx
                                  "Items"          item/mk-idx
                                  "Businesses"     biz/mk-idx}))]
    (doseq [a agents] (send a -update))
    (apply await agents)
    (println "done!")))

;; MAIN
(defn -main [& args]

  ;; elastic search
  (util/es-connect! cfg/elastic-search-cfg)
  
  ;; mongo
  (util/mongo-connect! cfg/mongo-db-cfg)

  ;; -- FOR BLOWING EVERYTHING AWAY --
  ;; (-rm-es-indices ["business_categories" "itemcategories" "businesses"])
  ;; (mg/drop-database! "centzy2_development")

  (-par-index-all))
