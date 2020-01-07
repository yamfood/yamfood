(ns yamfood.core.riders.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]))


(def rider-query
  {:select   [:riders.id :riders.tid :riders.name :riders.phone]
   :from     [:riders]
   :order-by [:riders.id]})


(defn rider-by-tid-query
  [tid]
  (hs/format (hh/merge-where rider-query [:= :riders.tid tid])))


(defn rider-by-tid!
  [tid]
  (->> (rider-by-tid-query tid)
       (jdbc/query db/db)
       (first)))
