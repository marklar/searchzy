(ns searchzy.service.authorization
  "Middleware: Authorizaton"
  (:require [searchzy
             [cfg :as cfg]]
            [searchzy.service
             [responses :as responses]]))

;; TODO: move into "middleware" namespace?

;; White list.
(def uris-not-requiring-auth
  #{"/"
    "/docs"
    "/docs/suggestions"
    "/docs/businesses"
    "/docs/business_menu_items"
    "/docs/lists"
    "/v1/suggestions"
    })

(defn- white-listed?
  [uri]
  (contains? uris-not-requiring-auth uri))

(defn- valid-key?
  [api-key]
  (let [lock (:api-key (cfg/get-cfg))]
    (if (nil? lock)
      true
      (= api-key lock))))

(defn- bounce []
   ;; Using "forbidden" (403) instead of "unauthorized" (401)
   ;; because I don't want to deal with authentication headers.
   (responses/forbidden-json {:error "Not authorized."}))

(defn- authorized?
  [request]
  (or (white-listed? (:uri request))
      (valid-key? (:api_key (:params request)))))

;;-------------------

(defn authorize
  "Check for valid API key, if necessary."
  [handler]
  (fn [request]
    (if-not (authorized? request)
      (bounce)
      (handler request))))
