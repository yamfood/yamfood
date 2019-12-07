(ns yamfood.core.baskets.core
  (:require
    [honeysql.core :as hs]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [yamfood.core.users.core :as users]
    [yamfood.core.products.core :as products]))


(defn- basket-products-query
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


(defn- basket-totals-query
  [basket-id]
  (format "
    select
      coalesce(sum(basket_products.count * products.price), 0) as total_cost,
      coalesce(sum(basket_products.count * products.energy), 0) as total_energy
    from basket_products, products
    where basket_products.basket_id = %d and
          products.id = basket_products.product_id", basket-id))


(defn- basket-products!
  [basket-id]
  (->> (basket-products-query basket-id)
       (jdbc/query db/db)))


(defn basket-totals!
  [basket-id]
  (->> (basket-totals-query basket-id)
       (jdbc/query db/db)
       (first)))


(defn basket-state!
  [basket-id]
  (let [basket (basket-totals! basket-id)]
    (assoc basket :products (basket-products! basket-id))))


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
  (products/state-for-product-detail! basket-id product-id))


(defn decrement-product-in-basket!
  [basket-id product-id]
  (->> (decrement-product-query basket-id product-id)
       (jdbc/execute! db/db))
  (products/state-for-product-detail! basket-id product-id))


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
    (products/state-for-product-detail! basket-id product-id)))


(defn pre-order-state!
  [basket-id]
  {:basket (basket-state! basket-id)
   :user   (users/user-with-basket-id! basket-id)})


(defn clear-basket!
  [basket-id]
  (jdbc/delete! db/db "basket_products" ["basket_id = ?" basket-id]))
