(defproject searchzy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [org.clojure/data.json "0.2.3"]
                 [congomongo "0.4.1"]
                 ]
  :plugins [[lein-ring "0.8.5"]]
  :ring {:handler searchzy.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.5"]]}})
