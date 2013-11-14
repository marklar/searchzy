(ns searchzy.index.core
  "For running the service from the command line using
   'lein run -m searchzy.index.core'."
  (:gen-class)
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

(defn- rm-index [name]
  (let [pre (str "index '" name "': ")]
    (if (es-idx/exists? name)
      (do (println (str pre "exists.  deleting."))
          (es-idx/delete name))
      (println (str pre "doesn't exist.")))))

(defn- get-idx-names []
  (map (fn [k m] (:index m)) cfg/elastic-search-names))

(defn- blow-away-everything []
  ;;(mg/drop-database! "centzy_web_production")
  (doseq [n (get-idx-names)]
    (rm-index n)))

;; -- indexing --

;;
;; Can use this:
;;     "Biz combined"   biz-combined/mk-idx
;; instead of "Biz Menu Items" and "Businesses".
;;
;; On my laptop, MongoDB fetching isn't the bottleneck,
;; so it takes the same amount of time.
;; 
(def indices
  [;; quick
   {:idx-name "Biz Categories", :f biz-cat/mk-idx, :db :main}
   {:idx-name "Items", :f item/mk-idx, :db :main}
   ;; slow
   {:idx-name "Combined", :f biz-combined/mk-idx, :db :businesses}
   ;; OR, do each slow independently.
   ;; {:idx-name "Businesses", :f biz/mk-idx, :db :businesses}
   ;; {:idx-name "Biz Menu Items", :f biz-menu-item/mk-idx, :db :businesses}
   ])

(defn index-one
  [{:keys [idx-name f db]}]
  (println (str "indexing: " idx-name))
  (let [c (:mongo-db (cfg/get-cfg))
        db-name (get (:db-names c) db)]
    (util/mongo-connect! db-name c)
    (let [cnt (f)]
      (println (str "indexed " cnt " " (str idx-name) " records."))
      cnt)))

;; -- public --

(defn index-all
  "Serial index creation."
  []
  (doseq [idx indices]
    (index-one idx)))

;; (defn par-index-all
;;   "Parallel indexing.
;;    On my laptop, this uses way too much memory and crashes the JVM.
;;    (Perhaps I just need to change the JVM's memory settings?)
;;    On Big Iron, using this indexing method may well work find and be faster."
;;   []
;;   (let [agents (map agent idx-name-2-fn)]
;;     (doseq [a agents] (send a index-one))
;;     (apply await agents)
;;     (println "done!")))

;; -- MAIN --
(defn -main
  [& args]
  (util/es-connect! (:elastic-search (cfg/get-cfg)))
  (index-all))
