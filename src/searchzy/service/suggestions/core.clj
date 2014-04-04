(ns searchzy.service.suggestions.core
  "For suggestions searches."
  (:require [searchzy.cfg :as cfg]
            [searchzy.service
             [util :as util]
             [inputs :as inputs]
             [responses :as responses]
             [business :as biz]]
            [searchzy.service.suggestions
             [response :as sugg-response]]
            [clojurewerkz.elastisch.native
             [document :as es-doc]]))

(defn- mk-biz-cat-id-filter
  [domain biz-cat-ids]
  (if (empty? biz-cat-ids)
    nil
    (let [field-name (if (= domain :business_categories)
                       :_id
                       :business_category_ids)]
      {:term {field-name biz-cat-ids}})))

(defn- mk-filtered-query
  [domain query-str biz-cat-ids]
  {:filtered {:query (util/mk-suggestion-query query-str)
              :filter (mk-biz-cat-id-filter domain biz-cat-ids)}})

(defn- get-results
  "Perform prefix search against names."
  [domain query-str biz-cat-ids {:keys [from size]}]
  (let [es-names (get cfg/elastic-search-names domain)]
    (:hits (es-doc/search (:index es-names)
                          (:mapping es-names)
                          :query  (mk-filtered-query domain query-str biz-cat-ids)
                          :from   from
                          :size   size))))

;;
;; TODO: return biz-categories (order: display_order ASC).
;; 

;; fetch results
;; TODO: in *parallel*.  How?
;;  - pmap: Probably not worth the coordination overhead.
;;  - clojure.core.reducers/map
;;  - agents (uncoordinated, asynchronous)
(defn- search
  [valid-args]
  (let [{:keys [endpoint query business-category-ids
                geo-map page-map utc-offset-map html]} valid-args]
    (let [no-q (clojure.string/blank? query)
          biz-results  (if no-q
                         {:total 0, :hits []}
                         (biz/es-search query :prefix
                                        business-category-ids
                                        geo-map nil ; -sort-
                                        page-map))
          item-results (if no-q
                         {:total 0, :hits []}
                         (get-results :items
                                      query business-category-ids page-map))
          cat-results  (get-results :business_categories
                                    query business-category-ids page-map)]
      (responses/ok-json
       (sugg-response/mk-response biz-results cat-results item-results
                                  endpoint query business-category-ids
                                  geo-map page-map utc-offset-map html)))))


;;-- public --

(defn mk-input-map
  [endpoint query business-category-ids address lat lon miles size html utc-offset]
  {:endpoint endpoint
   :query query
   :business-category-ids business-category-ids
   :geo-map {:address address, :lat lat, :lon lon, :miles miles}
   :page-map {:from "0", :size size}
   :utc-offset utc-offset
   :html html})

;; v1
(defn validate-and-search-v1
  "Perform 3 searches (in parallel!):
      - businesses (w/ filtering)
      - business_categories
      - items"
  [input-args]
  (util/validate-and-search input-args inputs/suggestion-clean-input-v1 search))

;; v2
(defn validate-and-search-v2
  "Perform 3 searches (in parallel!):
      - businesses (w/ filtering)
      - business_categories
      - items"
  [input-args]
  (util/validate-and-search input-args inputs/suggestion-clean-input-v2 search))
