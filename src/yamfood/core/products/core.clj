(ns yamfood.core.products.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]))


(defn- get-all-products-query []
  (hs/format {:select   [:id :name :price :photo :thumbnail :energy]
              :from     [:products]
              :order-by [:id]}))


(defn get-all-products! []
  (->> (get-all-products-query)
       (jdbc/query db/db)))


(def basket-cost-query "
  (select
    coalesce(sum(products.price * basket_products.count), 0)
  from basket_products,
       products
  where basket_products.basket_id = %d and
        products.id = basket_products.product_id) as basket_cost")


(defn product-detail-state-query
  [basket-id]
  {:select    [:products.id :products.name :products.price
               :products.photo :products.thumbnail
               :products.energy
               (hs/raw (format basket-cost-query basket-id))
               (hs/raw "coalesce(basket_products.count, 0) as count_in_basket")]
   :from      [:products]
   :order-by  [:id]
   :left-join [:basket_products [:and
                                 [:= :basket_products.basket_id basket-id]
                                 [:= :products.id :basket_products.product_id]]]
   :limit     1})


(defn- product-by-name-query
  [basket-id name]
  (-> (product-detail-state-query basket-id)
      (hh/merge-where [:= :products.name name])
      (hs/format)))


(defn- product-detail-state-by-id-query
  [basket-id product-id]
  (-> (product-detail-state-query basket-id)
      (hh/merge-where [:= :products.id product-id])
      (hs/format)))


(defn product-by-name!
  [basket-id name]
  (->> (product-by-name-query basket-id name)
       (jdbc/query db/db)
       (first)))


(defn state-for-product-detail!
  [basket-id id]
  (->> (product-detail-state-by-id-query basket-id id)
       (jdbc/query db/db)
       (first)))
