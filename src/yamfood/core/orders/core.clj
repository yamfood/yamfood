(ns yamfood.core.orders.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [yamfood.core.utils :as cu]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [yamfood.core.clients.core :as clients]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.core.kitchens.core :as kitchens]))


(def order-statuses
  {:new        "new"
   :on-kitchen "onKitchen"
   :on-way     "onWay"
   :finished   "finished"
   :canceled   "canceled"})


(def active-order-statuses
  [(:new order-statuses)
   (:on-kitchen order-statuses)
   (:on-way order-statuses)])


(def ended-order-statuses
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


(defn order-total-sum-query
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
  {:select    [:orders.id
               :orders.location
               :orders.payment
               [:kitchens.id :kitchen_id]
               [:kitchens.name :kitchen]
               [:kitchens.payload :kitchen_payload]
               :orders.created_at
               :clients.tid
               :clients.name
               :clients.phone
               [:riders.name :rider_name]
               [:riders.phone :rider_phone]
               [(order-total-sum-query :orders.id) :total_sum]
               [(order-current-status-query :orders.id) :status]
               :orders.comment
               :orders.address]
   :from      [:orders]
   :left-join [:riders [:= :orders.rider_id :riders.id]
               :clients [:= :orders.client_id :clients.id]
               :kitchens [:= :orders.kitchen_id :kitchens.id]]
   :order-by  [:id]})


(def order-products-query
  {:select [:products.id
            :products.name
            :products.price
            :products.payload
            :order_products.count
            [(hs/call :* :order_products.count :products.price) :total]]
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
       (jdbc/query db/db)
       (map cu/keywordize-field)))


(defn add-products!
  [order]
  (assoc order :products (products-by-order-id! (:id order))))


(defn orders-by-client-id-query
  [client-id]
  (hs/format (hh/merge-where order-detail-query [:= :orders.client_id client-id])))


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


(defn ended-orders-query
  [offset limit]
  {:with   [[:cte_orders order-detail-query]]
   :select [:*]
   :from   [:cte_orders]
   :where  [:in :cte_orders.status ended-order-statuses]
   :offset offset
   :limit  limit})


(defn ended-orders!
  ([]
   (ended-orders! 0 100))
  ([offset limit]
   (ended-orders! offset limit nil))
  ([offset limit where]
   (->> (-> (ended-orders-query offset limit)
            (hh/merge-where where))
        (hs/format)
        (jdbc/query db/db)
        (map fmt-order-location))))


(defn ended-orders-count!
  ([]
   (ended-orders-count! nil))
  ([where]
   (->> (-> (ended-orders-query 0 100)
            (dissoc :limit)
            (dissoc :offset)
            (assoc :select [:%count.cte_orders.id])
            (hh/merge-where where))
        (hs/format)
        (jdbc/query db/db)
        (first)
        (:count))))


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
                    (map #(cu/keywordize-field % :kitchen_payload))
                    (first))
         products? (:products? options)
         totals? (:totals? options)]
     (when order
       (merge (if totals? (order-totals! order-id) {})
              (if products? (add-products! order) {})
              order)))))


(defn orders-by-client-id!
  [client-id]
  (->> (orders-by-client-id-query client-id)
       (jdbc/query db/db)
       (map fmt-order-location)))


(defn accept-order!
  [order-id admin-id]
  (jdbc/insert!
    db/db "order_logs"
    {:order_id order-id
     :status   (:on-kitchen order-statuses)
     :payload  {:admin_id admin-id}}))


(defn cancel-order!
  [order-id]
  (jdbc/insert!
    db/db "order_logs"
    {:order_id order-id
     :status   (:canceled order-statuses)}))


(defn client-active-order!
  [client-id]
  (-> (orders-by-client-id! client-id)
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
  [client-id lon lat address comment kitchen-id payment]
  ["insert into orders (client_id, location, address, comment, kitchen_id, payment) values (?, POINT (?, ?), ?, ?, ?, ?) ;"
   client-id
   lon lat
   address
   comment
   kitchen-id
   payment])


(defn insert-order!
  [client-id lon lat address comment kitchen-id payment]
  (let [query (insert-order-query client-id
                                  lon lat
                                  address
                                  comment
                                  kitchen-id
                                  payment)]
    (jdbc/execute! db/db query {:return-keys ["id"]})))


(defn insert-products!
  [products]
  (jdbc/insert-multi! db/db "order_products" products))


(defn create-order!
  ; TODO: Use transaction!
  [basket-id location address comment payment]
  (let [client (clients/client-with-basket-id! basket-id)
        kitchen (kitchens/nearest-kitchen! (:longitude location)
                                           (:latitude location))
        order-id (:id (insert-order! (:id client)
                                     (:longitude location)
                                     (:latitude location)
                                     address
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


(defn pay-order!
  [order-id]
  (jdbc/with-db-transaction
    [t-con db/db]
    (jdbc/update! t-con "orders"
                  {:is_payed true}
                  ["id = ?" order-id])
    (jdbc/insert! t-con "order_logs"
                  {:order_id order-id
                   :status   (:new order-statuses)})))
