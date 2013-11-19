(ns searchzy.index.core
  "For running the service from the command line using
   'lein run -m searchzy.index.core'."
  (:gen-class)
  (:use [clojure.tools.cli :only [cli]])
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
  {;; quick
   :biz-categories {:f biz-cat/mk-idx,       :db :main}
   :items          {:f item/mk-idx,          :db :main}
   ;; slow
   :combined       {:f biz-combined/mk-idx,  :db :businesses}
   ;; OR, do each slow independently.
   :businesses     {:f biz/mk-idx,           :db :businesses}
   :biz-menu-items {:f biz-menu-item/mk-idx, :db :businesses}
   })

(defn- index-one
  [name]
  (let [idx (get indices name)]
    (if (nil? idx)
      ;; domain doesn't exist
      (println (str ">>> Invalid domain name: " name ". SKIPPING. <<<"))
      ;; okay
      (let [f   (:f idx)
            db  (:db idx)]
        (println (str "indexing: " name))
        (let [c (get (:mongo-db (cfg/get-cfg)) db)]
          (util/mongo-connect! c)
          (let [cnt (f)]
            (println (str "indexed " cnt " " (str name) " records."))
            cnt))))))

;; -- public --

(defn- index-all
  "Serial index creation."
  [domains]
  (let [names (if (= (first domains) "all")
                [:biz-categories :items :combined]
                (map keyword domains))]
    (doseq [n names]
      (index-one n))))

;; (defn par-index-all
;;   "Parallel indexing.  (Not for use with 'Combined' - no advantage.)
;;    On my laptop, this uses way too much memory and crashes the JVM.
;;    (Perhaps I just need to change the JVM's memory settings?)
;;    On Big Iron, using this indexing method may well work fine and be faster."
;;   []
;;   (let [agents (map agent indices)]
;;     (doseq [a agents] (send a index-one))
;;     (apply await agents)
;;     (println "done!")))

;; -- MAIN --
(defn -main
  [& args]

  (let [[args-map args-vec doc-str]
        (cli args
             (str "Searchzy Indexer.  For extracting MongoDB data and "
                  "indexing with ElasticSearch.  See .config.yaml for details.")
             ["-d" "--domains" (str "The domains to index. "
                                    "If multiple, ENCLOSE IN QUOTES. "
                                    "Options: "
                                    "all || "
                                    "subset: {"
                                    "biz-categories, "
                                    "items, "
                                    "businesses, "
                                    "biz-menu-items}")
              :parse-fn #(clojure.string/split % #"\s+")
              :default "all"])]
    (println doc-str)
    (println (:domains args-map))
    
    (util/es-connect! (:elastic-search (cfg/get-cfg)))
    (index-all (:domains args-map))))

