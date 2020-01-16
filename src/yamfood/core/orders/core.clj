(ns yamfood.core.orders.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [yamfood.core.baskets.core :as b]
    [yamfood.core.users.core :as users]))


(def order-statuses
  {:new        "new"
   :on-kitchen "on-kitchen"
   :ready      "ready"
   :on-way     "on-way"})


(defn fmt-order-location
  [order]
  (assoc order
    :location
    (db/point->clj (:location order))))


(defn- order-totals-query
  [order-id]
  (format "
    select
      coalesce(sum(order_products.count * products.price), 0) as total_cost,
      coalesce(sum(order_products.count * products.energy), 0) as total_energy
    from order_products, products
    where order_products.order_id = %d and
          products.id = order_products.product_id", order-id))


(defn order-totals!
  [order-id]
  (->> (order-totals-query order-id)
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
              :users.name
              :users.phone
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


(defn add-products
  [order]
  (assoc order :products (products-by-order-id! (:id order))))


(defn orders-by-user-id-query
  [user-id]
  (hs/format (hh/merge-where order-detail-query [:= :orders.user_id user-id])))


(defn active-order-by-rider-id-query
  [rider-id]
  (hs/format
    {:with   [[:cte_orders (hh/merge-where
                             order-detail-query
                             [:= :orders.rider_id rider-id])]]
     :select [:*]
     :from   [:cte_orders]
     :where  [:= :cte_orders.status "on-way"]}))


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
              (if products? (add-products order) {})
              order)))))


(defn orders-by-user-id!
  [user-id]
  (->> (orders-by-user-id-query user-id)
       (jdbc/query db/db)
       (map fmt-order-location)))


(defn user-active-order!
  [user-id]
  (-> (orders-by-user-id! user-id)
      (last)
      (add-products)))


(defn products-from-basket!
  [basket-id]
  (->> (products-from-basket-query basket-id)
       (jdbc/query db/db)))


(defn prepare-basket-products-to-order
  [basket-products order-id]
  (map #(assoc % :order_id order-id) basket-products))


(defn insert-order-query
  [user-id lon lat comment]
  ["insert into orders (user_id, location, comment) values (?, POINT(?, ?), ?);"
   user-id
   lon lat
   comment])


(defn insert-order!
  [user-id lon lat comment]
  (let [query (insert-order-query user-id
                                  lon lat
                                  comment)]
    (jdbc/execute! db/db query {:return-keys ["id"]})))


(defn insert-products!
  [products]
  (jdbc/insert-multi! db/db "order_products" products))


(defn create-order-and-clear-basket!
  [basket-id location comment]
  (let [user (users/user-with-basket-id! basket-id)
        order (insert-order! (:id user)
                             (:longitude location)
                             (:latitude location)
                             comment)]
    (-> (products-from-basket! basket-id)
        (prepare-basket-products-to-order (:id order))
        (insert-products!))
    (jdbc/insert!
      db/db "order_logs"
      {:order_id (:id order)
       :status   (:new order-statuses)})
    (b/clear-basket! basket-id)))


(defn assign-rider-to-order!
  ; TODO: Log to order_logs and check order statuses before assigning
  [order-id rider-id]
  (jdbc/update! db/db "orders" {:rider_id rider-id} ["id = ?" order-id]))
