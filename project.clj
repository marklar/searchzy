(defproject searchzy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.json "0.2.4"]
                 [org.clojure/core.match "0.2.1"]
                 [org.clojure/core.cache "0.6.3"]
                 [org.clojure/tools.cli "0.3.1"]
                 [camel-snake-kebab "0.1.4"]
                 [clj-yaml "0.4.0"]
                 [clj-time "0.7.0"]
                 [compojure "1.1.6"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [hiccup "1.0.4"]
                 [congomongo "0.4.3"]
                 [clojurewerkz/elastisch "2.0.0-beta3"]
                 [geocoder-clj "0.2.3"]]

  ;; INDEX: "lein run -m searchy.index.core
  ;; :main searchzy.index.core

  ;; SERVER, prod mode: "lein run"
  :main searchzy.service.core
  :aot [searchzy.service.core]

  ;; SERVER, dev mode: "lein ring server"
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler searchzy.service.handler/app}

  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}})
