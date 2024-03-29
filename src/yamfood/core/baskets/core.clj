(ns yamfood.core.baskets.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [yamfood.core.utils :as cu]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [yamfood.core.clients.core :as clients]
    [yamfood.core.products.core :as products]))


(def basket-products-query
  {:select    [:products.id
               :basket_products.count
               [:basket_products.id :bp_id]
               :basket_products.payload
               :products.name
               :products.price
               :categories.is_delivery_free
               :products.energy]
   :from      [:basket_products :products]
   :where     [:and
               [:= :products.id :basket_products.product_id]]
   :left-join [:categories [:= :products.category_id :categories.id]]
   :order-by  [:id]})


(defn add-modifiers!
  [all-modifiers]
  (fn [product]
    (assoc product
      :modifiers
      (map (products/get-modifier all-modifiers)
           (:modifiers (:payload product))))))


(defn calculate-total-cost
  [product]
  (let [modifiers (:modifiers product)
        modifiers-cost (reduce + (map :price modifiers))
        price (:price product)]
    (assoc product :total_cost (+ price modifiers-cost))))


(defn- disabled-products-query [basket-id kitchen-id]
  (hs/format
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
                  [:not :products.is_active]]]}))


(defn- remove-basket-products-query
  [product-ids]
  (hs/format
    {:delete-from :basket_products
     :where       [:in :product_id product-ids]}))


(defn- basket-products!
  [basket-id]
  (let [all-modifiers (products/modifiers!)]
    (->> (-> basket-products-query
             (hh/merge-where [:= :basket_products.basket_id basket-id])
             (hs/format))
         (jdbc/query db/db)
         (map #(-> %
                   (cu/keywordize-field :name)
                   (cu/keywordize-field :payload)
                   ((add-modifiers! all-modifiers))
                   (calculate-total-cost))))))


(defn basket-totals!
  [basket-id]
  (->> (-> (products/basket-totals-query basket-id)
           (hs/format))
       (jdbc/query db/db)
       (map #(assoc % :total_energy 0))
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


(defn add-product-to-basket!
  ([basket-id product-id]
   (add-product-to-basket! basket-id product-id []))
  ([basket-id product-id modifiers]
   (let [basket-product (-> (jdbc/insert! db/db "basket_products"
                                          {:product_id product-id
                                           :basket_id  basket-id
                                           :payload    {:modifiers modifiers}})
                            (first))]
     (products/state-for-product-detail! basket-id (:product_id basket-product)))))


(defn order-confirmation-state!
  [basket-id]
  {:basket (basket-state! basket-id)
   :client (clients/client-with-basket-id! basket-id)})


(defn clear-basket!
  [basket-id]
  (jdbc/delete! db/db "basket_products" ["basket_id = ?" basket-id]))


(defn basket-product-detail!
  [basket-product-id]
  (let [all-modifiers (products/modifiers!)]
    (->> (-> basket-products-query
             (hh/merge-where [:= :basket_products.id basket-product-id])
             (hs/format))
         (jdbc/query db/db)
         (map #(-> %
                   (cu/keywordize-field :name)
                   (cu/keywordize-field :payload)
                   ((add-modifiers! all-modifiers))))
         (first))))


(defn remove-basket-products! [product-ids]
  (jdbc/execute! db/db (remove-basket-products-query product-ids)))


(defn disabled-basket-products!
  [basket-id kitchen-id]
  (->> (disabled-products-query basket-id kitchen-id)
       (jdbc/query db/db)
       (map #(cu/keywordize-field % :name))))


(defn update-owner!
  [busket-id client-id]
  (jdbc/update! db/db "baskets" {:client_id client-id} ["id = ?" busket-id]))
