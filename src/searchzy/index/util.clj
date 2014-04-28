(ns searchzy.index.util
  (:require [clojurewerkz.elastisch.native.index :as es-idx]
            [clojure.set]))

(defn file->lines
  "Given name of file, split on newlines and return strings.
   :: str -> [str]
  "
  [file-name]
  (-> file-name
      slurp
      (clojure.string/split #"\s+")))

(defn recreate-idx
  "If index 'idx-name' exists, delete it.
   Then create it using 'mapping-types'.
   ...
   Do this only if indexing from scratch.
   If updating index, do not do this."
  [idx-name mapping-types]
  ;; TODO
  ;; Functions delete and create return Clojure maps.
  ;; Use response/ok? or response/conflict? to verify success.
  (if (es-idx/exists? idx-name)
    (do
      (println (str "deleting index: " idx-name))
      (es-idx/delete idx-name)))
  (do
    (println (str "creating index: " idx-name))
    (let [foo (es-idx/create idx-name :mappings mapping-types)]
      (println foo))))
