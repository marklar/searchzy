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

(defn -index-one
  [[name f]]
  (println (str "indexing: " name))
  (let [cnt (f)]
    (println (str "indexed " cnt " " (str name) " records."))
    cnt))

(def idx_name_2_fn {"Biz Menu Items" biz-menu-item/mk-idx
                    "Biz Categories" biz-cat/mk-idx
                    "Items"          item/mk-idx
                    "Businesses"     biz/mk-idx})

(defn -index-all
  "Serial index creation."
  []
  (doseq [pair idx_name_2_fn]
    (-index-one pair)))

(defn -par-index-all
  "Parallel indexing.
   On my laptop, this uses way too much memory and crashes the JVM.
   (Perhaps I just need to change the JVM's memory settins?)
   On Big Iron, using this indexing method may well work find and be faster."
  []
  (let [agents (map agent idx_name_2_fn)]
    (doseq [a agents] (send a -index-one))
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

  (-index-all))
