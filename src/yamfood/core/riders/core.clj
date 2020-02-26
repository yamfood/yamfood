(ns yamfood.core.riders.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [yamfood.core.orders.core :as o]))


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
       (:sum)))


(defn orders-sum!                                           ; TODO: Make it work!
  [rider-id]
  140000)


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
