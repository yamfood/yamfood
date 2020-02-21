(ns yamfood.core.orders.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [yamfood.core.users.core :as users]
    [yamfood.core.kitchens.core :as kitchens]
    [yamfood.telegram.handlers.utils :as u]))


(def order-statuses
  {:new        "new"
   :on-kitchen "onKitchen"
   :ready      "ready"
   :on-way     "onWay"
   :finished   "finished"
   :canceled   "canceled"})


(def active-order-statuses
  [(:new order-statuses)
   (:on-kitchen order-statuses)
   (:ready order-statuses)
   (:on-way order-statuses)])


(def finished-order-statuses
  [(:canceled order-statuses)
   (:finished order-statuses)])


(def cancelable-order-statuses
  [(:new order-statuses)])


(defn fmt-order-location
  [order]
  (assoc order
    :location
    (db/point->clj (:location order))))


(defn- order-totals-query
  [order-id]
  {:select [(hs/raw "coalesce(sum(order_products.count * products.price), 0) as total_cost")
            (hs/raw "coalesce(sum(order_products.count * products.energy), 0) as total_energy")]
   :from   [:order_products :products]
   :where  [:and
            [:= :order_products.order_id order-id]
            [:= :products.id :order_products.product_id]]})


(defn- order-total-sum-query
  [order-id]
  {:select [(hs/raw "coalesce(sum(order_products.count * products.price), 0) as total_cost")]
   :from   [:order_products :products]
   :where  [:and
            [:= :order_products.order_id order-id]
            [:= :products.id :order_products.product_id]]})


(defn order-totals!
  [order-id]
  (->> (order-totals-query order-id)
       (hs/format)
       (jdbc/query db/db)
       (first)))


(defn- products-from-basket-query
  [basket-id]
  (hs/format {:select [:product_id :count]
              :from   [:basket_products]
              :where  [:= :basket_id basket-id]}))


(defn order-current-status-query
  [order-id]
  {:select   [:order_logs.status]
   :from     [:order_logs]
   :where    [:= :order_logs.order_id order-id]
   :order-by [[:order_logs.created_at :desc]]
   :limit    1})


(def order-detail-query
  {:select   [:orders.id
              :orders.location
              :orders.created_at
              :users.name
              :users.phone
              [(order-total-sum-query :orders.id) :total_sum]
              [(order-current-status-query :orders.id) :status]
              :orders.comment]
   :from     [:orders :users]
   :where    [:= :orders.user_id :users.id]
   :order-by [:id]})


(def order-products-query
  {:select [:products.name
            :products.price
            :order_products.count]
   :from   [:order_products :products]
   :where  [:= :order_products.product_id :products.id]})


(defn products-by-order-id-query
  [order-id]
  (hs/format (hh/merge-where
               order-products-query
               [:= :order_products.order_id order-id])))


(defn products-by-order-id!
  [order-id]
  (->> (products-by-order-id-query order-id)
       (jdbc/query db/db)))


(defn add-products!
  [order]
  (assoc order :products (products-by-order-id! (:id order))))


(defn orders-by-user-id-query
  [user-id]
  (hs/format (hh/merge-where order-detail-query [:= :orders.user_id user-id])))


(def active-orders-query
  {:with   [[:cte_orders order-detail-query]]
   :select [:*]
   :from   [:cte_orders]
   :where  [:in :cte_orders.status active-order-statuses]})


(defn active-orders!
  []
  (->> active-orders-query
       (hs/format)
       (jdbc/query db/db)
       (map fmt-order-location)))


(def finished-orders-query
  {:with   [[:cte_orders order-detail-query]]
   :select [:*]
   :from   [:cte_orders]
   :where  [:in :cte_orders.status finished-order-statuses]})


(defn finished-orders!
  []
  (->> finished-orders-query
       (hs/format)
       (jdbc/query db/db)
       (map fmt-order-location)))


(defn active-order-by-rider-id-query
  [rider-id]
  (hs/format
    {:with   [[:cte_orders (hh/merge-where
                             order-detail-query
                             [:= :orders.rider_id rider-id])]]
     :select [:*]
     :from   [:cte_orders]
     :where  [:= :cte_orders.status " on-way "]}))


(defn active-order-by-rider-id!
  [rider-id]
  (let [order (->> (active-order-by-rider-id-query rider-id)
                   (jdbc/query db/db)
                   (first))]
    (when order
      (fmt-order-location order))))


(defn order-by-id-query
  [order-id]
  (hs/format (hh/merge-where order-detail-query [:= :orders.id order-id])))


(def order-by-id-options {:products? true :totals? true})
(defn order-by-id!
  ; TODO: Write docs for this function!
  ([order-id]
   (order-by-id! order-id order-by-id-options))
  ([order-id options]
   (let [options (merge options order-by-id-options)
         order (->> (order-by-id-query order-id)
                    (jdbc/query db/db)
                    (map fmt-order-location)
                    (first))
         products? (:products? options)
         totals? (:totals? options)]
     (when order
       (merge (if totals? (order-totals! order-id) {})
              (if products? (add-products! order) {})
              order)))))


(defn orders-by-user-id!
  [user-id]
  (->> (orders-by-user-id-query user-id)
       (jdbc/query db/db)
       (map fmt-order-location)))


(defn cancel-order!
  [order-id]
  (jdbc/insert!
    db/db "order_logs"
    {:order_id order-id
     :status   (:canceled order-statuses)}))


(defn user-active-order!
  [user-id]
  (-> (orders-by-user-id! user-id)
      (last)
      (#(order-by-id! (:id %)))))


(defn products-from-basket!
  [basket-id]
  (->> (products-from-basket-query basket-id)
       (jdbc/query db/db)))


(defn prepare-basket-products-to-order
  [basket-products order-id]
  (map #(assoc % :order_id order-id) basket-products))


(defn insert-order-query
  [user-id lon lat comment kitchen-id payment]
  ["insert into orders (user_id, location, comment, kitchen_id, payment) values (?, POINT (?, ?), ?, ?, ?) ;"
   user-id
   lon lat
   comment
   kitchen-id
   payment])


(defn insert-order!
  [user-id lon lat comment kitchen-id payment]
  (let [query (insert-order-query user-id
                                  lon lat
                                  comment
                                  kitchen-id
                                  payment)]
    (jdbc/execute! db/db query {:return-keys ["id"]})))


(defn insert-products!
  [products]
  (jdbc/insert-multi! db/db "order_products" products))


(defn create-order!
  ; TODO: Use transaction!
  [basket-id location comment payment]
  (let [user (users/user-with-basket-id! basket-id)
        kitchen (kitchens/nearest-kitchen! (:longitude location)
                                           (:latitude location))
        order-id (:id (insert-order! (:id user)
                                     (:longitude location)
                                     (:latitude location)
                                     comment
                                     (:id kitchen)
                                     payment))]
    (-> (products-from-basket! basket-id)
        (prepare-basket-products-to-order order-id)
        (insert-products!))
    (when (= payment (:value u/cash-payment))
      (jdbc/insert!
        db/db "order_logs"
        {:order_id order-id
         :status   (:new order-statuses)}))
    order-id))
