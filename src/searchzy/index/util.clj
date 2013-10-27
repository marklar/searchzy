(ns searchzy.index.util
  (:require [clojurewerkz.elastisch.native.index :as es-idx]
            [clojure.set]))

(defn recreate-idx
  "If index 'idx-name' exists, delete it. Then create it using 'mapping-types'."
  [idx-name mapping-types]
  ;; TODO
  ;; Functions delete and create return Clojure maps.
  ;; Use response/ok? or response/conflict? to verify success.
  (if (es-idx/exists? idx-name)
    (es-idx/delete idx-name))
  (es-idx/create idx-name :mappings mapping-types))

