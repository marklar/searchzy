(ns searchzy.index.core
  "For running the service from the command line using
   'lein run -m searchzy.index.core'."
  (:gen-class)
  (:require [clojure.tools.cli :as cli]
            [clj-time
             [core :as t]
             [format :as f]]
            [searchzy
             [util :as util]
             [cfg :as cfg]]
            [searchzy.index.domains :as domains]
            [clojure.string :as str]
            [clojurewerkz.elastisch.native.index :as es-idx]))

;; -- deleting --

(defn- rm-index [name]
  (let [pre (str "index '" name "':")]
    (if (es-idx/exists? name)
      (do (println pre "exists.  deleting.")
          (es-idx/delete name))
      (println pre "doesn't exist."))))

(defn- get-idx-names []
  (map (fn [_ m] (:index m))
       cfg/elastic-search-names))

(defn- blow-away-everything []
  ;; (mg/drop-database! "centzy_web_production")
  (doseq [n (get-idx-names)]
    (rm-index n)))

;; -- indexing --

(defn- index-one
  "Given a domain-name,
   connect to the corresponding MongoDB collection,
   and call the corresponding indexing function."
  [domain-name & {:keys [limit after ids-file]}]
  ;; index :: map w/ keys :index-fn, :db-name
  (let [idx (domains/name->index domain-name)]
    (if (nil? idx)
      ;; domain doesn't exist
      (println (str ">>> Invalid domain name: " domain-name ". SKIPPING. <<<"))
      ;; okay
      (let [{:keys [index-fn db-name]} idx]
        (println "indexing:" domain-name)
        (util/mongo-connect-db! db-name)
        (let [cnt (index-fn :limit limit :after after :ids-file ids-file)]
          (println "indexed" cnt (str domain-name) "records.")
          cnt)))))

(defn- index-all
  "Serial index creation."
  [domains-str & {:keys [limit after ids-file]}]
  (doseq [name (domains/str->names domains-str)]
    (index-one name :limit limit :after after :ids-file ids-file)))

;; (defn- par-index-all
;;   "Parallel indexing.  (Not for use with 'Combined' - no advantage.)
;;    On my laptop, this uses way too much memory and crashes the JVM.
;;    (Perhaps I just need to change the JVM's memory settings?)
;;    On Big Iron, using this indexing method may well work fine and be faster."
;;   []
;;   (let [agents (map agent domains/indices)]
;;     (doseq [a agents] (send a index-one))
;;     (apply await agents)
;;     (println "done!")))

(def cli-options
  [
   ;; help
   ["-h"
    "--help"
    "Displays this help text and exits."
    :flag true]
   
   ;; limit (for testing, really)
   ["-l"
    "--limit NUM"
    "For testing.  Limit number of records to index (per domain)."
    :default "nil"]

   ;; domains
   ["-d"
    "--domains \"dom1 dom2\""
    "The domains to index.  If multiple, ENCLOSE IN QUOTES."
    ;; :parse-fn #(str/split % #"\s+")
    :default "all"]

   ;; biz-ids file (for updating)
   ["-f"
    "--file BIZ-ID-FILE"
    (str "For {businesses|biz-menu-items|combined}, "
         "file of BusinessIDs (1/line) to update.")
    :default nil]

   ["-a"
    "--after yyyyMMdd"
    "Add/update only those records updated after DATE (start of day, UTC)."
    :default nil]

   ])

(defn- usage [options-summary]
  (->>
   ["Searchzy Indexer."
    "For extracting MongoDB data and indexing with ElasticSearch."
    ""
    "Options:"
    options-summary
    ""
    (str "Indexible domains: {" domains/all-names "}.\n")
    ]
   (str/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn- parse-date-utc
  "Given string of format yyyyMMdd, return start-of-day DateTime in UTC.
  :: str -> DateTime"
  [str]
  (.toDate
   (f/parse (f/formatter "yyyyMMdd") str)))

(defn- parse-date-eastern
  "Given string of format yyyyMMdd, return start-of-day DateTime for NY.
  :: str -> DateTime"
  [str]
  (.toDate 
   (t/from-time-zone
    (f/parse (f/formatter "yyyyMMdd") str)
    (t/time-zone-for-id "America/New_York"))))

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
      ;; (not= 1 (count arguments)) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))

    ;; execute program w/ options.
    (do
      (util/es-connect! (:elastic-search (cfg/get-cfg)))
      (index-all (:domains options)
                 :limit (read-string (:limit options))
                 :after (if-let [after (:after options)]
                          (parse-date-utc after)
                          nil)
                 :ids-file (:file options)))))
