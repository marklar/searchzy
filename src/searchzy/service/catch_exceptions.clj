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
      (catch Throwable t
        (responses/error-json {:endpoint (:uri request)
                               :params (:params request)
                               :error (str t)
                               :stack_trace (map str (.getStackTrace t))
                               })))))
