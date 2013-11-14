(defproject searchzy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.3"]
                 [org.clojure/core.match "0.2.0"]
                 [clj-yaml "0.4.0"]
                 [compojure "1.1.5"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [hiccup "1.0.4"]
                 [congomongo "0.4.1"]
                 [clojurewerkz/elastisch "1.2.0"]
                 [geocoder-clj "0.2.3"]]

  ;; INDEX: "lein run -m searchy.index.core
  ;; :main searchzy.index.core

  ;; SERVER, prod mode: "lein run"
  :main searchzy.service.core
  :aot [searchzy.service.core]

  ;; SERVER, dev mode: "lein ring server"
  :plugins [[lein-ring "0.8.7"]]
  :ring {:handler searchzy.service.handler/app}

  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}})
