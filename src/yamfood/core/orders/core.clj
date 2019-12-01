(ns yamfood.core.orders.core
  (:require [clojure.java.jdbc :as jdbc]
            [honeysql.core :as hs]
            [yamfood.core.db.core :as db]
            [yamfood.core.users.core :as users]
            [yamfood.core.baskets.core :as b]))


(defn- products-from-basket-query
  [basket-id]
  (hs/format {:select [:product_id :count]
              :from   [:basket_products]
              :where  [:= :basket_id basket-id]}))


(defn products-from-basket!
  [basket-id]
  (->> (products-from-basket-query basket-id)
       (jdbc/query db/db)))


(defn prepare-basket-products-to-order
  [basket-products order-id]
  (map #(assoc % :order_id order-id) basket-products))


(defn insert-order-query
  [user-id lat lon comment]
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
    (b/clear-basket! basket-id)))


