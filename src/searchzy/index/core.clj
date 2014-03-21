(ns searchzy.index.core
  "For running the service from the command line using
   'lein run -m searchzy.index.core'."
  (:gen-class)
  (:use [clojure.tools.cli :only [cli]])
  (:require [searchzy
             [util :as util]
             [cfg :as cfg]]
            [clojure.string :as str]
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
  ;; (mg/drop-database! "centzy_web_production")
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
;; We don't use the 'areas' DB?
;;
(def indices
  {
   ;; PRETTY QUICK
   :biz-categories {:db-name :main
                    :index-fn biz-cat/mk-idx}
                    
   :items          {:db-name :main
                    :index-fn item/mk-idx}
                    
   ;; TAKE A LONG TIME

   ;; both :businesses and :biz-menu-items together, or...
   :combined       {:db-name :businesses
                    :index-fn biz-combined/mk-idx}
                    
   ;; ...each of them independently.
   :businesses     {:db-name :businesses
                    :index-fn biz/mk-idx}
   :biz-menu-items {:db-name :businesses
                    :index-fn biz-menu-item/mk-idx}
   })


(defn- mongo-problem-str
  [exception]
  (clojure.string/join
   "\n"
   ["Problem connecting to MongoDB:"
    (str exception)
    "Please ensure that MongoDB is running."]))

(defn- mongo-connect-db!
  "Given a MongoDB collection name,
   attempt to establish connection to it."
  [db-name]
  ;; This just assumes that the db-name will be correct.
  ;; FIXME: add check.
  (let [cfc (get (:mongo-db (cfg/get-cfg)) db-name)]
    (util/mongo-connect! cfc)))

(defn- index-one
  "Given a domain-name,
   connect to the corresponding MongoDB collection,
   and call the corresponding indexing function."
  [domain-name & {:keys [limit]}]
  (let [idx (get indices domain-name)]
    (if (nil? idx)
      ;; domain doesn't exist
      (println (str ">>> Invalid domain name: " domain-name ". SKIPPING. <<<"))
      ;; okay
      (let [{:keys [index-fn db-name]} idx]
        (println (str "indexing: " domain-name))
        (try (do (mongo-connect-db! db-name)
                 (let [cnt (index-fn :limit limit)]
                   (println (str "indexed " cnt " " (str domain-name) " records."))
                   cnt))
             (catch Exception e
               (println (mongo-problem-str e))))))))

(defn- words
  "Given string of space- (and possibly comma-) separated values,
   return an iSeq of 'words'."
  [s]
  (-> s
      str/trim
      (str/split #"\s*,?\s")))

;; -- public --

(defn- index-all
  "Serial index creation."
  [domains-str & {:keys [limit]}]
  (let [domains (words domains-str)
        names (if (= domains ["all"])
                [:biz-categories :items :combined]
                (map keyword domains))]
    (doseq [n names]
      (index-one n :limit limit))))

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
             (str "\nSearchzy Indexer.\n"
                  "For extracting MongoDB data and "
                  "indexing with ElasticSearch.\n\n"
                  "Indexible domains: {biz-categories, items, "
                  "businesses, biz-menu-items}.")

             ["-h" "--help" "Displays this help text and exits."
              :flag true]

             ["-l" "--limit" "Limit records to index (each domain)."
              :default "nil"]

             ["-d" "--domains" (str "The domains to index. "
                                    "If multiple, ENCLOSE IN QUOTES.")
              ;; :parse-fn #(clojure.string/split % #"\s+")
              :default "all"]
             )]

    (if (:help args-map)
      (println doc-str)
      (do
        (util/es-connect! (:elastic-search (cfg/get-cfg)))
        (index-all (:domains args-map)
                   :limit (read-string (:limit args-map)))))))
