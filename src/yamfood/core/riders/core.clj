(ns yamfood.core.riders.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [yamfood.core.orders.core :as o]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.helpers.status :as status]
    [yamfood.telegram.helpers.feedback :as feedback])
  (:import
    (java.time LocalDateTime)))


(defn withdraw-from-balance!
  [rider-id admin-id amount description]
  (jdbc/insert!
    db/db
    "rider_balance"
    {:rider_id    rider-id
     :amount      (- amount)
     :admin_id    admin-id
     :description description}))


(defn deposit-to-balance!
  ([rider-id admin-id amount description]
   (deposit-to-balance!
     db/db rider-id admin-id amount description))
  ([db rider-id admin-id amount description]
   (jdbc/insert!
     db
     "rider_balance"
     {:rider_id    rider-id
      :amount      amount
      :admin_id    admin-id
      :description description})))


(defn finished-orders-query
  [rider-id]
  {:select [:order_logs.order_id]
   :from   [:order_logs :orders]
   :where  [:and
            [:= :order_logs.order_id :orders.id]
            [:= :order_logs.status (:finished o/order-statuses)]
            [:= (hs/raw "(order_logs.payload->>'rider_id')::numeric") rider-id]]})


(defn balance-query
  [rider-id]
  {:select [[(hs/raw "coalesce(sum(rider_balance.amount), 0)::bigint") :sum]]
   :from   [:rider_balance]
   :where  [:= :rider_balance.rider_id rider-id]})


(defn balance-log-query
  [rider-id]
  {:select    [:rider_balance.*
               [:admins.name :admin_name]
               [:admins.login :admin_login]]
   :from      [:rider_balance]
   :left-join [:admins [:= :rider_balance.admin_id :admins.id]]
   :where     [:= :rider_balance.rider_id rider-id]
   :order-by  [[:created_at :desc]]})


(defn total-balance-logs
  [rider-id]
  {:select [:%count.id]
   :from   [:rider_balance]
   :where  [:= :rider_balance.rider_id rider-id]})


(defn balance-logs! [rider-id limit offset]
  (->> (-> (balance-log-query rider-id)
           (assoc :limit limit
                  :offset offset)
           (hs/format))
       (jdbc/query db/db)))


(defn count-balance-logs! [rider-id]
  (->> (-> (total-balance-logs rider-id)
           (hs/format))
       (jdbc/query db/db)
       (first)
       :count))


(defn current-balance!
  [rider-id]
  (->> (-> (balance-query rider-id)
           (hs/format))
       (jdbc/query db/db)
       (first)
       (:sum)))


(defn earned-money-today!
  [rider-id]
  (let [today (.toLocalDate (LocalDateTime/now))]
    (->> (-> (balance-query rider-id)
             (hh/merge-where [:> :rider_balance.amount 0])
             (hh/merge-where [:> :rider_balance.created_at today])
             (hs/format))
         (jdbc/query db/db)
         (first)
         (:sum))))


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
  (->> (rider-by-tid-query tid)
       (jdbc/query db/db)
       (first)))


(defn rider-by-id!
  [id]
  (->> (-> rider-list-query
           (hh/merge-where [:= :riders.id id]))
       (hs/format)
       (jdbc/query db/db)
       (map #(assoc % :balance (current-balance! (:id %))))
       (first)))


(defn rider-by-phone!
  [phone]
  (->> (-> rider-detail-query
           (hh/merge-where [:= :riders.phone phone]))
       (hs/format)
       (jdbc/query db/db)
       (first)))


(defn finished-orders-today-count!
  [rider-id]
  (let [today (.toLocalDate (LocalDateTime/now))]
    (->> (-> (finished-orders-query rider-id)
             (assoc :select [(hs/raw "count(distinct order_id)")])
             (hh/merge-where [:> :order_logs.created_at today]))
         (hs/format)
         (jdbc/query db/db)
         (first)
         (:count))))


(defn menu-state!
  [rider-id]
  (let [rider (rider-by-id! rider-id)
        finished-orders-today (finished-orders-today-count! rider-id)]
    (merge rider {:finished-orders-today finished-orders-today
                  :earned-money-today    (earned-money-today! rider-id)})))


(defn update!
  [id row]
  (jdbc/update!
    db/db
    "riders"
    row
    ["riders.id = ?" id]))


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
    (jdbc/update! t-con "orders" {:rider_id rider-id} ["id = ?" order-id])
    (status/notify-order-on-way! order-id)))


(defn finish-order!
  [order rider-id]
  (let [products (:products order)
        delivery_cost (reduce max (map :rider_delivery_cost products))]
    (jdbc/with-db-transaction
      [t-con db/db]
      (jdbc/insert! t-con "order_logs"
                    {:order_id (:id order)
                     :status   (:finished o/order-statuses)
                     :payload  (db/map->jsonb {:rider_id rider-id})})
      (deposit-to-balance!
        t-con
        rider-id
        nil
        delivery_cost
        (str "Оплата за заказ №" (:id order))))
    (feedback/send-feedback-request! (:id order))))


(defn cancel-order!
  [order-id rider-id]
  (jdbc/with-db-transaction
    [t-con db/db]
    (jdbc/insert!
      t-con
      "order_logs"
      {:order_id order-id
       :status   (:on-kitchen o/order-statuses)
       :payload  (db/map->jsonb {:rider_id rider-id})})
    (jdbc/update!
      t-con
      "orders"
      {:rider_id nil}
      ["orders.rider_id = ?" rider-id])))
