(ns searchzy.core
  (:require [somnium.congomongo :as mg]
            [searchzy.business :as business]
            [searchzy.item-category :as item-category]
            [searchzy.business-category :as business-category]))

;; MONGO CONNECTION (global)
(def conn
  (mg/make-connection "centzy2_development"
                      :host "127.0.0.1"
                      :port 27017))
(mg/set-connection! conn)

(defn -build-index
  "Call 'f'.  Print out number of iterations (cnt)."
  [f name]
  (println (str "Indexing " name "..."))
  (let [cnt (f)]
    (println (str "Indexed " cnt " " name " records."))))

(defn -main [& args]
  (-build-index business/mk-idx "Businesses")
  (-build-index item-category/mk-idx "ItemCategories")
  (-build-index business-category/mk-idx "BusinessCategories"))
  
