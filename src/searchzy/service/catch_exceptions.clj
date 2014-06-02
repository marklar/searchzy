(ns searchzy.service.catch-exceptions
  "Middleware: Uncaught Exceptions"
  (:require [searchzy.service
             [responses :as responses]]))

;; TODO: move into "middleware" namespace?

(defn catch-exceptions
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (responses/error-json {:endpoint (:uri request)
                               :params (:params request)
                               :error (str e)
                               })))))
