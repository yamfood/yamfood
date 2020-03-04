(ns yamfood.core.riders.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [yamfood.core.orders.core :as o])
  (:import (java.util Date)
           (java.time LocalDateTime)))


(defn make-deposit!
  [rider-id admin-id amount]
  (first
    (jdbc/insert!
      db/db
      "rider_deposits"
      {:rider_id rider-id
       :admin_id admin-id
       :amount   amount})))


(defn deposits-sum-query
  [rider-id]
  {:select   [:%sum.rider_deposits.amount]
   :from     [:rider_deposits]
   :where    [:= :rider_deposits.rider_id rider-id]
   :group-by [:rider_deposits.rider_id]})


(defn deposits-sum!
  [rider-id]
  (->> (deposits-sum-query rider-id)
       (hs/format)
       (jdbc/query db/db)
       (first)
       (:sum)
       (or 0)))


(defn finished-orders-query
  [rider-id]
  {:select [:order_logs.order_id]
   :from   [:order_logs]
   :where  [:and
            [:= :order_logs.status (:finished o/order-statuses)]
            [:= (hs/raw "(order_logs.payload->>'rider_id')::numeric") rider-id]]})


(defn orders-sum!
  [rider-id]
  (->> {:select [(hs/raw "coalesce(sum(order_products.count * products.price), 0) as total_cost")]
        :from   [:order_products :products]
        :where  [:and
                 [:in :order_products.order_id (finished-orders-query rider-id)]
                 [:= :products.id :order_products.product_id]]}
       (hs/format)
       (jdbc/query db/db)))


(defn calculate-deposit!
  [rider-id]
  (let []
    (- (deposits-sum! rider-id)
       (orders-sum! rider-id))))


(def rider-list-query
  {:select   [:riders.id
              :riders.tid
              :riders.name
              :riders.phone
              :riders.notes
              :riders.is_blocked]
   :from     [:riders]
   :order-by [:riders.id]})


(def rider-detail-query
  {:select   [:riders.id
              :riders.tid
              :riders.name
              :riders.phone
              :riders.notes
              :riders.is_blocked]
   :from     [:riders]
   :order-by [:riders.id]})


(defn rider-by-tid-query
  [tid]
  (hs/format (hh/merge-where rider-detail-query [:= :riders.tid tid])))


(defn rider-by-tid!
  [tid]
  (let [rider (->> (rider-by-tid-query tid)
                   (jdbc/query db/db)
                   (first))]
    (assoc rider
      :active-order
      (o/active-order-by-rider-id! (:id rider)))))


(defn rider-by-id!
  [id]
  (->> (-> rider-list-query
           (hh/merge-where [:= :riders.id id]))
       (hs/format)
       (jdbc/query db/db)
       (map #(assoc % :deposit (calculate-deposit! (:id %))))
       (first)))


(defn rider-by-phone!
  [phone]
  (->> (-> rider-detail-query
           (hh/merge-where [:= :riders.phone phone]))
       (hs/format)
       (jdbc/query db/db)
       (first)))


(defn finished-orders-count-query
  [rider-id]
  (-> (finished-orders-query rider-id)
      (assoc :select [:%count.order_logs.id])))


(defn finished-orders-today-count!
  [rider-id]
  (let [today (.toLocalDate (LocalDateTime/now))]
    (->> (-> (finished-orders-count-query rider-id)
             (hh/merge-where [:> :created_at today]))
         (hs/format)
         (jdbc/query db/db)
         (first)
         (:count))))


(defn menu-state!
  [rider-id]
  (let [rider (rider-by-id! rider-id)
        finished-orders-today (finished-orders-today-count! rider-id)]
    (merge rider {:finished-orders-today finished-orders-today
                  :earned-money-today    (* finished-orders-today 10000)})))


(defn update!
  [id row]
  (jdbc/update!
    db/db
    "riders"
    row
    ["riders.id = ?" id]))



(update! 2 {:notes "test"})



(defn limited-rider-query
  [offset limit]
  (merge rider-list-query {:offset offset :limit limit}))


(defn all-riders!
  ([]
   (all-riders! 0 100 nil))
  ([offset limit]
   (all-riders! offset limit nil))
  ([offset limit search]
   (->> (-> (limited-rider-query offset limit)
            (hh/merge-where search))
        (hs/format)
        (jdbc/query db/db))))


(defn riders-count!
  ([]
   (riders-count! nil))
  ([where]
   (->> (-> rider-list-query
            (assoc :select [[:%count.riders.id :count]])
            (dissoc :order-by)
            (hh/merge-where where))
        (hs/format)
        (jdbc/query db/db)
        (first)
        (:count))))


(defn new-rider!
  [rider]
  (first
    (jdbc/insert!
      db/db
      "riders"
      rider
      {:return-keys ["id"]})))


(defn assign-rider-to-order!
  [order-id rider-id]
  (jdbc/with-db-transaction
    [t-con db/db]
    (jdbc/insert! t-con "order_logs"
                  {:order_id order-id
                   :status   (:on-way o/order-statuses)
                   :payload  (db/map->jsonb {:rider_id rider-id})})
    (jdbc/update! t-con "orders" {:rider_id rider-id} ["id = ?" order-id])))


(defn finish-order!
  [order-id rider-id]
  (jdbc/with-db-transaction
    [t-con db/db]
    (jdbc/insert! t-con "order_logs"
                  {:order_id order-id
                   :status   (:finished o/order-statuses)
                   :payload  (db/map->jsonb {:rider_id rider-id})})))


(defn cancel-order!
  [order-id rider-id]
  (jdbc/with-db-transaction
    [t-con db/db]
    (jdbc/insert! t-con "order_logs" {:order_id order-id
                                      :status   (:canceled-by-rider o/order-statuses)
                                      :payload  (db/map->jsonb {:rider_id rider-id})})))
