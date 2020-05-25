(ns yamfood.core.baskets.core
  (:require
    [honeysql.core :as hs]
    [yamfood.core.utils :as cu]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [honeysql-postgres.format :refer :all]
    [yamfood.core.kitchens.core :as kitchens]
    [yamfood.core.products.core :as products]))


(defn- basket-products-query
  [basket-id]
  (hs/format {:select    [:products.id
                          :basket_products.count
                          :products.name
                          :products.price
                          :categories.is_delivery_free
                          :products.energy]
              :from      [:basket_products :products]
              :where     [:and
                          [:= :basket_products.basket_id basket-id]
                          [:= :products.id :basket_products.product_id]]
              :left-join [:categories [:= :products.category_id :categories.id]]
              :order-by  [:id]}))


(defn- disabled-products [basket-id kitchen-id]
  {:select    [:products.*, :categories.emoji, :basket_products.count]
   :from      [:baskets]
   :join      [:basket_products [:= :baskets.id :basket_products.basket_id]
               :products [:= :basket_products.product_id :products.id]]
   :left-join [:categories [:= :products.category_id :categories.id]
               :disabled_products [:= :basket_products.product_id :disabled_products.product_id]]
   :where     [:and
               [:= :baskets.id basket-id]
               [:= :disabled_products.kitchen_id kitchen-id]
               [:or
                [:!= :disabled_products nil]
                [:not :products.is_active]]]})


(defn- disabled-products-query [basket-id kitchen-id]
  (hs/format (disabled-products basket-id kitchen-id)))


(defn- remove-basket-products-query
  [product-ids]
  (hs/format
    (-> {:delete-from :basket_products
         :where       [:in :product_id product-ids]
         :returning   [:*]})
    :parameterizer :none))


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
       (jdbc/query db/db)
       (map #(cu/keywordize-field % :name))))


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


(defn clear-basket!
  [basket-id]
  (jdbc/delete! db/db "basket_products" ["basket_id = ?" basket-id]))


(defn remove-basket-products! [product-ids]
  (jdbc/query db/db (remove-basket-products-query product-ids)))

;; TODO remove unused
(defn remove-disabled-basket-products! [basket-id kitchen-id]
  (jdbc/query db/db (remove-basket-products-query (-> (disabled-products basket-id kitchen-id)
                                                      (assoc :select [:products.id])))))


(defn disabled-basket-products!
  [basket-id kitchen-id]
  (->> (disabled-products-query basket-id kitchen-id)
       (jdbc/query db/db)
       (map #(cu/keywordize-field % :name))))


(defn order-confirmation-state!
  [client]
  (merge (when-let [kitchen (kitchens/nearest-kitchen! (:bot_id client) (:longitude client) (:latitude client))]
           {:kitchen           kitchen
            :disabled_products (disabled-basket-products! (:basket_id client) (:id kitchen))})
         {:basket (basket-state! (:basket_id client))
          :client client}))
