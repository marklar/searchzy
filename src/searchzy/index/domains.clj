(ns searchzy.index.domains
  (:require [clojure.string :as str]
            [searchzy.index
             [biz-combined :as biz-combined]
             [business :as biz]
             [item :as item]
             [location :as location]
             [list :as list]
             [business-menu-item :as biz-menu-item]
             [business-category :as biz-cat]]))

(defn- words
  "Given string of space- (and possibly comma-) separated values,
   return an iSeq of 'words'."
  [s]
  (-> s
      str/trim
      (str/split #"\s+|(\s*,\s*)")))

;;
;; Can use this:
;;     "Biz combined"   biz-combined/mk-idx
;; instead of "Biz Menu Items" and "Businesses".
;;
;; On my laptop, MongoDB fetching isn't the bottleneck,
;; so it takes the same amount of time.
;; 
;; We don't use the 'areas' DB?
;;
(def indices
  {
   ;; -- PRETTY QUICK --
   :biz-categories {:db-name :main
                    :index-fn biz-cat/mk-idx}
                    
   :items          {:db-name :main
                    :index-fn item/mk-idx}

   :locations      {:db-name :areas
                    :index-fn location/mk-idx}

   ;; -- TAKE A LONG TIME --

   :lists          {:db-name :areas
                    :index-fn list/mk-idx}
                    
   ;; both :businesses and :biz-menu-items together, or...
   :combined       {:db-name :businesses
                    :index-fn biz-combined/mk-idx}
                    
   ;; ...each of them independently.
   :businesses     {:db-name :businesses
                    :index-fn biz/mk-idx}
   :biz-menu-items {:db-name :businesses
                    :index-fn biz-menu-item/mk-idx}
   })

(def all-names (->> indices
                    keys
                    (map name)
                    sort
                    (str/join ", ")))

(def all-domains [:biz-categories :items :locations :lists :combined])

(defn str->names
  [domains-str]
  (let [domains (words domains-str)]
    (if (= domains ["all"])
      all-domains
      (map keyword domains))))

(defn name->index
  [domain-name]
  (get indices domain-name))
