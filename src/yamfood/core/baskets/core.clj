(ns yamfood.core.baskets.core
  (:require [yamfood.core.products.core :as p]
            [yamfood.core.db.core :as db]
            [honeysql.core :as hs]
            [clojure.java.jdbc :as jdbc]))


(defn- get-basket-products-query
  [basket-id]
  (hs/format {:select   [:products.id
                         :basket_products.count
                         :products.name
                         :products.price
                         :products.energy]
              :from     [:basket_products :products]
              :where    [:and
                         [:= :basket_products.basket_id basket-id]
                         [:= :products.id :basket_products.product_id]]
              :order-by [:id]}))


(defn- get-basket-totals-query
  [basket-id]
  (format "
    select
      coalesce(sum(basket_products.count * products.price), 0) as total_cost,
      coalesce(sum(basket_products.count * products.energy), 0) as total_energy
    from basket_products, products
    where basket_products.basket_id = %d and
          products.id = basket_products.product_id", basket-id))


(defn- get-basket-products!
  [basket-id]
  (->> (get-basket-products-query basket-id)
       (jdbc/query db/db)))

(defn get-basket-totals!
  [basket-id]
  (->> (get-basket-totals-query basket-id)
       (jdbc/query db/db)
       (first)))

(defn get-basket-state!
  [basket-id]
  (let [basket (get-basket-totals! basket-id)]
    (assoc basket :products (get-basket-products! basket-id))))


(defn increment-product-query
  [basket-id product-id]
  (hs/format {:update :basket_products
              :set    {:count (hs/raw "count + 1")}
              :where  [:and
                       [:= :basket_id basket-id]
                       [:= :product_id product-id]]}))


(defn decrement-product-query
  [basket-id product-id]
  (hs/format {:update :basket_products
              :set    {:count (hs/raw "count - 1")}
              :where  [:and
                       [:= :basket_id basket-id]
                       [:= :product_id product-id]]}))


(defn increment-product-in-basket!
  [basket-id product-id]
  (->> (increment-product-query basket-id product-id)
       (jdbc/execute! db/db))
  (p/get-state-for-product-detail! basket-id product-id))


(defn decrement-product-in-basket!
  [basket-id product-id]
  (->> (decrement-product-query basket-id product-id)
       (jdbc/execute! db/db))
  (p/get-state-for-product-detail! basket-id product-id))


(defn- insert-product-to-basket!
  [basket-id product-id]
  (first
    (jdbc/insert! db/db "basket_products"
                  {:product_id product-id
                   :basket_id  basket-id})))


(defn add-product-to-basket!
  [basket-id product-id]
  (let [product-id (:product_id (insert-product-to-basket!
                                  basket-id
                                  product-id))]
    (p/get-state-for-product-detail! basket-id product-id)))


(defn make-order-state!
  [basket-id]
  {:basket (get-basket-state! basket-id)})

