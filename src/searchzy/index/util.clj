(ns searchzy.index.util
  (:require [clojurewerkz.elastisch.native.index :as es-idx]
            [clojure.set]))

(defn rm-leading-underbar
  "Remove the leading underbar from _id."
  [mg-map]
  (clojure.set/rename-keys mg-map {:_id :id}))

(defn recreate-idx
  [idx-name mapping-types]
  (es-idx/delete idx-name)
  (if (not (es-idx/exists? idx-name))
    (es-idx/create idx-name :mappings mapping-types)))

