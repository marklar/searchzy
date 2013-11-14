(ns searchzy.service.flurbl
  "Code taken from Elastisch and modified to allow a GeoDistanceSortBuilder."
  (:import [org.elasticsearch.action.search SearchRequest]
           [org.elasticsearch.search.builder SearchSourceBuilder]
           [org.elasticsearch.search.sort GeoDistanceSortBuilder SortOrder]
           [org.elasticsearch.common.unit DistanceUnit]
           java.util.Map)
  (:require [clojure.walk :as wlk]
            [clojurewerkz.elastisch
             [native :as es]]
            [clojurewerkz.elastisch.native
             [conversion :as cnv]]))

(defn- ^"[Ljava.lang.String;" ->string-array
  "Coerces argument to an array of strings"
  [index-name]
  (if (coll? index-name)
    (into-array String index-name)
    (into-array String [index-name])))


(defn ^SearchRequest ->search-request
  [index-name mapping-type {:keys [search-type search_type scroll routing
                                   preference
                                   query facets from size timeout filter
                                   min_score version fields sort stats] :as options}]
  (let [r                       (SearchRequest.)
        ^SearchSourceBuilder sb (SearchSourceBuilder.)]

    ;; source
    (when query
      (.query sb ^Map (wlk/stringify-keys query)))
    (when facets
      (.facets sb ^Map (wlk/stringify-keys facets)))
    (when from
      (.from sb from))
    (when size
      (.size sb size))
    (when timeout
      (.timeout sb ^String timeout))
    (when filter
      (.filter sb ^Map (wlk/stringify-keys filter)))
    (when fields
      (.fields sb ^java.util.List fields))
    (when version
      (.version sb version))
    (when sort
      ;; (set-sort sb sort))
      (.sort sb sort))  ;; 'sort' has to be a SortBuilder!
    (when stats
      (.stats sb ->string-array stats))
    (.source r sb)

    ;; non-source
    (when index-name
      (.indices r (->string-array index-name)))
    (when mapping-type
      (.types r (->string-array mapping-type)))
    (when-let [s (or search-type search_type)]
      (.searchType r ^String s))
    (when routing
      (.routing r ^String routing))
    (when scroll
      (.scroll r ^String scroll))

    r))


;; If Elastisch knew how to build a GeoDistanceSortBuilder
;; from this, then we'd be good.  But it doesn't.
;; (defn- old-mk-geo-sort
;;   [coords order]
;;   (let [lat-lon-str (str (:lat coords) "," (:lon coords))]
;;     {:_geo_distance {:latitude_longitude lat-lon-str
;;                      :order order
;;                      :unit "mi"}}))

(defn mk-geo-distance-sort-builder
  "Create a GeoDistanceSortBuilder"
  [coords order]
  (let [o  (if (= order :asc) SortOrder/ASC SortOrder/DESC)
        sb (GeoDistanceSortBuilder. "latitude_longitude")] 
    (doto sb
      (.point (:lat coords) (:lon coords))
      (.order o)
      (.unit DistanceUnit/MILES))
    sb))

(defn- get-order-and-attr
  [sort-str]
  (if (clojure.string/blank? sort-str)
    [:desc "value"]
    (let [s (clojure.string/trim sort-str)]
      (if (= \- (first s))
        [:desc, (apply str (rest s))]
        [:asc,  s]))))

(defn get-sort-map
  "If a non-legal value is supplied, return nil (== error)."
  [sort-str valid-attributes]
  (let [[order attr] (get-order-and-attr sort-str)]
    (if (contains? valid-attributes attr)
      {:attribute attr, :order order}
      nil)))

(defn distance-sort-search
  "Use this instead of es-doc/search if you need to
   use a GeoDistanceSortBuilder."
  [index mapping-type & {:as options}]
  (let [ft (es/search (->search-request index mapping-type options))
        ^SearchResponse res (.get ft)]
    (cnv/search-response->seq res)))
