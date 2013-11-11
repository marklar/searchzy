(ns searchzy.service.core
  "For running the service from the command line using 'lein run PORT'."
  (:gen-class)
  (:require [searchzy.service.handler :as handler]
            [ring.adapter.jetty :as jetty]))

(def DEF_PORT 3000)

(defn get-port
  "Try, in order: [1] cmd line, [2] env var, [3] default."
  [port-str]
  (Integer. (or port-str
                (System/getenv "PORT")
                DEF_PORT)))

(defn -main [& [port-str]] 
  (jetty/run-jetty handler/app {:port (get-port port-str) :join? false}))
