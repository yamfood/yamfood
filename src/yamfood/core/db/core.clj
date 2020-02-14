(ns yamfood.core.db.core
  (:require
    [environ.core :refer [env]]
    [clj-postgresql.core :as pg]
    [clj-postgresql.types :as pgt]))


(def db {:classname      "org.postgresql.Driver"
         :connection-uri (env :jdbc-database-url)})


(defn point->clj
  [point]
  (let [point (pg/point point)]
    {:longitude (.x point)
     :latitude  (.y point)}))


(defn map->jsonb
  [map]
  (pgt/map->parameter map :jsonb))
