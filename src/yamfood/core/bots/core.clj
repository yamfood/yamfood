(ns yamfood.core.bots.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]))


(def bots-query
  {:select   [:bots.id
              :bots.token
              :bots.is_active
              :bots.payments_token
              :bots.name]
   :from     [:bots]
   :where    [:= :bots.is_active true]
   :order-by [:bots.id]})


(defn bot-by-token!
  [token]
  (->> (-> bots-query
           (hh/merge-where [:= :bots.token token])
           (hs/format))
       (jdbc/query db/db)
       (first)))


(defn bot-by-destination!
  [destination]
  (->> (-> bots-query
           (hh/merge-where [:= :bots.asterisk_destination (str destination)])
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


(defn all-bots!
  []
  (->> (-> bots-query
           (assoc :select [:bots.id :bots.name])
           (hs/format))
       (jdbc/query db/db)))
