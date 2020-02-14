(ns yamfood.core.regions.core
  (:require [honeysql.core :as hs]
            [clojure.java.jdbc :as jdbc]
            [clojure.data.json :as json]
            [yamfood.core.db.core :as db]))


(defn- region-by-location-query
  [lon lat]
  (hs/format
    {:select   [:regions.id :regions.name]
     :from     [:regions]
     :where    (hs/raw (format "st_contains(geometry(regions.polygon), geometry(point(%s, %s)))" lon lat))
     :order-by [[:regions.id :desc]]}))


(defn region-by-location!
  [lon lat]
  (->> (region-by-location-query lon lat)
       (jdbc/query db/db)
       (first)))


(defn all-regions-query
  []
  (hs/format
    {:select   [:regions.id :regions.name (hs/raw "st_asgeojson(geometry(regions.polygon)) as polygon")]
     :from     [:regions]
     :order-by [[:regions.id :desc]]}))


(defn jsonify-polygon
  [region]
  (assoc region
    :polygon
    (json/read-str (:polygon region))))


(defn all-regions!
  []
  (let [regions (->> (all-regions-query)
                     (jdbc/query db/db))]
    (map jsonify-polygon regions)))
