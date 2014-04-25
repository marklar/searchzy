(ns searchzy.index.core
  "For running the service from the command line using
   'lein run -m searchzy.index.core'."
  (:gen-class)
  (:require [clojure.tools.cli :as cli]
            [searchzy
             [util :as util]
             [cfg :as cfg]]
            [clojure.string :as str]
            [clojurewerkz.elastisch.native.index :as es-idx]
            [searchzy.index
             [biz-combined :as biz-combined]
             [business :as biz]
             [item :as item]
             [list :as list]
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
   ;; -- PRETTY QUICK --
   :biz-categories {:db-name :main
                    :index-fn biz-cat/mk-idx}
                    
   :items          {:db-name :main
                    :index-fn item/mk-idx}

   ;; -- TAKE A LONG TIME --

   :lists          {:db-name :locality-web-areas ;; areas
                    :index-fn list/mk-idx}
                    
   ;; both :businesses and :biz-menu-items together, or...
   :combined       {:db-name :businesses
                    :index-fn biz-combined/mk-idx}
                    
   ;; ...each of them independently.
   :businesses     {:db-name :businesses
                    :index-fn biz/mk-idx}
   :biz-menu-items {:db-name :businesses
                    :index-fn biz-menu-item/mk-idx}
   })

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
        (util/mongo-connect-db! db-name)
        (let [cnt (index-fn :limit limit)]
          (println (str "indexed " cnt " " (str domain-name) " records."))
          cnt)))))

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
                [:biz-categories :items :lists :combined]
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

(def cli-options
  [
   ["-h" "--help" "Displays this help text and exits."
    :flag true]
   
   ["-l" "--limit NUM" "Limit records to index (each domain)."
    :default "nil"]
   
   ["-d" "--domains \"dom1 dom2\"" (str "The domains to index. "
                                        "If multiple, ENCLOSE IN QUOTES.")
    ;; :parse-fn #(str/split % #"\s+")
    :default "all"]
   ]
  )

(defn- usage [options-summary]
  (->>
   ["Searchzy Indexer."
    "For extracting MongoDB data and indexing with ElasticSearch."
    ""
    "Options:"
    options-summary
    ""
    (str "Indexible domains: {"
         (str/join ", " (sort (map name (keys indices))))
         "}.")
    ]
   (str/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

;; -- MAIN --
(defn -main
  [& args]

  ;; TODO: Add error handling.
  ;; If user supplies some un-recognized flag (e.g. '-limit'),
  ;; indicate the error and output the doc-str.

  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options)]

    ;; handle help & error conditions.
    (cond
      (:help options) (exit 0 (usage summary))
      ;; (not= (count arguments) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))

    ;; execute program w/ options.
    (do
      (util/es-connect! (:elastic-search (cfg/get-cfg)))
      (index-all (:domains options)
                 :limit (read-string (:limit options))))))
