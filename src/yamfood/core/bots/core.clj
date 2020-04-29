(ns yamfood.core.bots.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]))


(def bots-query
  {:select [:bots.id
            :bots.token
            :bots.name]
   :from   [:bots]
   :where  [:= :bots.is_active true]})


(defn bot-by-token!
  [token]
  (->> (-> bots-query
           (hh/merge-where [:= :bots.token token])
           (hs/format))
       (jdbc/query db/db)
       (first)))


(defn bot-by-id!
  [id]
  (->> (-> bots-query
           (hh/merge-where [:= :bots.id id])
           (hs/format))
       (jdbc/query db/db)
       (first)))

