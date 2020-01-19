(ns yamfood.core.riders.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [yamfood.core.orders.core :as o]))


(def rider-query
  {:select   [:riders.id
              :riders.tid
              :riders.name
              :riders.phone]
   :from     [:riders]
   :order-by [:riders.id]})


(defn rider-by-tid-query
  [tid]
  (hs/format (hh/merge-where rider-query [:= :riders.tid tid])))


(defn rider-by-tid!
  [tid]
  (let [rider (->> (rider-by-tid-query tid)
                   (jdbc/query db/db)
                   (first))]
    (assoc rider
      :active-order
      (o/active-order-by-rider-id! (:id rider)))))


(defn assign-rider-to-order!
  [order-id rider-id]
  (jdbc/with-db-transaction
    [t-con db/db]
    (jdbc/insert! t-con "order_logs" {:order_id order-id
                                      :status   (:on-way o/order-statuses)
                                      :payload  (db/map->jsonb {:rider_id rider-id})})
    (jdbc/update! t-con "orders" {:rider_id rider-id} ["id = ?" order-id])))


(defn finish-order!
  [order-id rider-id]
  (jdbc/with-db-transaction
    [t-con db/db]
    (jdbc/insert! t-con "order_logs" {:order_id order-id
                                      :status   (:finished o/order-statuses)
                                      :payload  (db/map->jsonb {:rider_id rider-id})})))
