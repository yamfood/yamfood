(ns yamfood.core.db.core
  (:require
    [mount.core :as mount]
    [jdbc.pool.c3p0 :as pool]
    [environ.core :refer [env]]
    [clj-postgresql.core :as pg]
    [clj-postgresql.types :as pgt]))


(mount/defstate db
  :start (pool/make-datasource-spec
           {:classname      "org.postgresql.Driver"
            :connection-uri (env :jdbc-database-url)})
  :stop (.close db))


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
