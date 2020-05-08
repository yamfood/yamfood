(ns yamfood.core.db.core
  (:require
    [jdbc.pool.c3p0 :as pool]
    [environ.core :refer [env]]
    [clj-postgresql.core :as pg]
    [clj-postgresql.types :as pgt]))


(def db (pool/make-datasource-spec
          {:classname      "org.postgresql.Driver"
           :connection-uri (env :jdbc-database-url)}))


(defn point->clj
  [point]
  (let [point (pg/point point)]
    {:longitude (.x point)
     :latitude  (.y point)}))


(defn clj->point
  [location]
  (pg/point (:longitude location)
            (:latitude location)))


(defn map->jsonb
  [map]
  (pgt/map->parameter map :jsonb))
