(ns searchzy.index.util
  (:require [clojurewerkz.elastisch.native.index :as es-idx]
            [somnium.congomongo :as mg]
            [clojure.set]))

(defn file->lines
  "Given name of file, split on newlines and return strings.
   If no such file, throws exception.
   :: str -> [str]
  "
  [file-name]
  (-> file-name
      slurp
      (clojure.string/split #"\s+")))

(defn file->ids
  ":: str -> [objID]"
  [file-name]
  (if-not file-name
    []
    (let [biz-id-strs (file->lines file-name)]
      ;; I don't know hy 'str' here is necessary!!!!!
      (map #(mg/object-id (str %)) biz-id-strs))))

(defn- maybe-delete-idx [idx-name]
  (if (es-idx/exists? idx-name)
    (do
      (println (str "deleting index: " idx-name))
      (es-idx/delete idx-name))))

(defn recreate-idx
  "If index 'idx-name' exists, delete it.
   Then create it using :mappings.
   ...
   Do this only if indexing from scratch.
   If updating index, do not do this."
  [idx-name mapping-types]
  ;; TODO
  ;; Functions delete and create return Clojure maps.
  ;; Use response/ok? or response/conflict? to verify success.
  (maybe-delete-idx idx-name)
  (do
    (println (str "creating index: " idx-name))
    (let [foo (es-idx/create idx-name :mappings mapping-types)]
      (println foo))))
