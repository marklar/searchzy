(defproject searchzy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.3"]
                 [compojure "1.1.5"]
                 [congomongo "0.4.1"]
                 [clojurewerkz/elastisch "1.2.0"]
                 ]
  :plugins [[lein-ring "0.8.7"]]
  :main searchzy.core
  :ring {:handler searchzy.handler/app}
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}})
