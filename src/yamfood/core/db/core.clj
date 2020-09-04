(ns yamfood.core.db.core
  (:require
    [mount.core :as mount]
    [honeysql.format :refer :all]
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


(defmethod fn-handler "str-in"
  [_ column str-seq]
  (let [clause
        (let [str-seq (filter some? str-seq)]
          (str
            (to-sql column)
            " in "
            (to-sql (if (seq str-seq) str-seq [""]))))]
    (if (some nil? str-seq)
      (str "(" (to-sql column) " is null or " clause ")")
      clause)))


(defmethod fn-handler "->>"
  [_ column s]
  (str
    (to-sql column)
    " ->> "
    (to-sql s)))