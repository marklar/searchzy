(ns searchzy.index.core
  (:require [searchzy
             [util :as util]
             [cfg :as cfg]]
            [clojurewerkz.elastisch.native.index :as es-idx]
            [searchzy.index
             [biz-combined :as biz-combined]
             [business :as biz]
             [item :as item]
             [business-menu-item :as biz-menu-item]
             [business-category :as biz-cat]]))

;; -- deleting --

(defn -rm-index [name]
  (let [pre (str "index '" name "': ")]
    (if (es-idx/exists? name)
      (do (println (str pre "exists.  deleting."))
          (es-idx/delete name))
      (println (str pre "doesn't exist.")))))

(defn -get-idx-names []
  (map (fn [k m] (:index m)) cfg/elastic-search-names))

(defn -blow-away-everything []
  ;;(mg/drop-database! "centzy_web_production")
  (doseq [n (-get-idx-names)]
    (-rm-index n)))

;; -- indexing --

(defn -index-one
  [[name f]]
  (println (str "indexing: " name))
  (let [cnt (f)]
    (println (str "indexed " cnt " " (str name) " records."))
    cnt))

;;
;; Can use this:
;;     "Biz combined"   biz-combined/mk-idx
;; instead of "Biz Menu Items" and "Businesses",
;; but it takes the same amount of time.
;; Speed is limited not by MongoDB fetching, but by ES indexing.
;; 
(def idx_name_2_fn {
                    ;; quick
                    "Biz Categories" biz-cat/mk-idx
                    "Items"          item/mk-idx
                    ;; slow
                    "Businesses"     biz/mk-idx
                    "Biz Menu Items" biz-menu-item/mk-idx
                    })

(defn -index-all
  "Serial index creation."
  []
  (doseq [pair idx_name_2_fn]
    (-index-one pair)))

(defn -par-index-all
  "Parallel indexing.
   On my laptop, this uses way too much memory and crashes the JVM.
   (Perhaps I just need to change the JVM's memory settings?)
   On Big Iron, using this indexing method may well work find and be faster."
  []
  (let [agents (map agent idx_name_2_fn)]
    (doseq [a agents] (send a -index-one))
    (apply await agents)
    (println "done!")))

;; MAIN
(defn -main [& args]
  (util/es-connect! (:elastic-search (cfg/get-cfg)))
  (util/mongo-connect! (:mongo-db (cfg/get-cfg)))
  ;; (-blow-away-everything) 
  (-index-all))
