(ns yamfood.core.products.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [yamfood.core.utils :as cu]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]))


(def all-products-query
  {:select    [:products.id
               :products.name
               :products.price
               :products.photo
               :products.position
               :products.thumbnail
               :products.energy
               :products.category_id
               :categories.emoji
               [:categories.name :category]]
   :from      [:products]
   :where     [:= :is_active true]
   :left-join [:categories
               [:= :categories.id :products.category_id]]
   :order-by  [:categories.position :products.position]})


(defn disabled-products-query
  [kitchen-id]
  {:select [:disabled_products.product_id]
   :from   [:disabled_products]
   :where  [:= :disabled_products.kitchen_id kitchen-id]})


(defn all-products!
  ([]
   (->> all-products-query
        (hs/format)
        (jdbc/query db/db)
        (map #(cu/keywordize-field % :category))))
  ([kitchen-id]
   (->> (-> all-products-query
            (hh/merge-where
              [:not [:in
                     :products.id
                     (disabled-products-query kitchen-id)]]))
        (hs/format)
        (jdbc/query db/db)
        (map #(cu/keywordize-field % :category)))))


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
               :categories.emoji
               [:categories.name :category]
               (hs/raw (format basket-cost-query basket-id))
               (hs/raw "coalesce(basket_products.count, 0) as count_in_basket")]
   :from      [:products]
   :where     [:= :products.is_active true]
   :order-by  [:id]
   :left-join [:categories [:= :categories.id :products.category_id]
               :basket_products [:and
                                 [:= :basket_products.basket_id basket-id]
                                 [:= :products.id :basket_products.product_id]]]
   :limit     1})


(defn- product-detail-state-by-name-query
  [basket-id name]
  (-> (product-detail-state-query basket-id)
      (hh/merge-where [:= :products.name name])
      (hs/format)))


(defn- product-detail-state-by-id-query
  [basket-id product-id]
  (-> (product-detail-state-query basket-id)
      (hh/merge-where [:= :products.id product-id])
      (hs/format)))


(defn product-detail-state-by-name!
  [basket-id name]
  (->> (product-detail-state-by-name-query basket-id name)
       (jdbc/query db/db)
       (first)
       (#(cu/keywordize-field % :category))))


(defn state-for-product-detail!
  [basket-id id]
  (->> (product-detail-state-by-id-query basket-id id)
       (jdbc/query db/db)
       (first)
       (#(cu/keywordize-field % :category))))


(def categories-list-query
  {:select   [:categories.id
              :categories.name
              :categories.emoji]
   :from     [:categories]
   :order-by [:categories.position]})


(defn product-by-name!
  [name]
  (->> (-> all-products-query
           (hh/merge-where [:= :products.name name]))
       (hs/format)
       (jdbc/query db/db)
       (first)
       (#(cu/keywordize-field % :category))))


(defn product-by-id!
  [id]
  (->> (-> all-products-query
           (hh/merge-where [:= :products.id id]))
       (hs/format)
       (jdbc/query db/db)
       (first)
       (#(cu/keywordize-field % :category))))


(defn all-categories!
  []
  (->> categories-list-query
       (hs/format)
       (jdbc/query db/db)
       (map #(cu/keywordize-field % :name))))


(defn products-by-category-emoji!
  [emoji]
  (->> (-> all-products-query
           (hh/merge-where [:= :categories.emoji emoji]))
       (hs/format)
       (jdbc/query db/db)))


(defn create-product!
  [product]
  (first
    (jdbc/insert!
      db/db
      "products"
      product)))


(defn update!
  [product-id row]
  (jdbc/update!
    db/db
    "products"
    row
    ["products.id = ?" product-id]))


(defn delete!
  [product-id]
  (jdbc/update!
    db/db
    "products"
    {:is_active false}
    ["products.id = ?" product-id]))
