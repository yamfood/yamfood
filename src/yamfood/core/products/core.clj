(ns yamfood.core.products.core
  (:require [yamfood.core.db.core :as db]
            [honeysql.core :as hs]
            [honeysql.helpers :as hh]
            [clojure.java.jdbc :as jdbc]))


(defn- get-all-products-query []
  (hs/format {:select   [:id :name :price :photo :thumbnail :energy]
              :from     [:products]
              :order-by [:id]}))

(defn get-all-products! []
  (->> (get-all-products-query)
       (jdbc/query db/db)))


(def bucket-cost-query "
  (select
    (products.price * bucket_products.count)
  from bucket_products,
       products
  where bucket_products.bucket_id = %d and
        products.id = bucket_products.product_id) as bucket_cost")


(defn product-detail-state-query
  [bucket-id]
  {:select   [:products.id :products.name :products.price
              :products.photo :products.thumbnail
              :products.energy
              (hs/raw (format bucket-cost-query bucket-id))
              (hs/raw "coalesce(bucket_products.count, 0) as count_in_bucket")]
   :from     [:products]
   :order-by [:id]
   :left-join [:bucket_products [:and
                                 [:= :bucket_products.bucket_id bucket-id]
                                 [:= :products.id :bucket_products.product_id]]]
   :limit    1})

(defn- get-product-by-name-query
  [bucket-id name]
  (-> (product-detail-state-query bucket-id)
    (hh/merge-where [:= :products.name name])
    (hs/format)))


(defn- get-product-detail-state-by-id-query
  [bucket-id product-id]
  (-> (product-detail-state-query bucket-id)
      (hh/merge-where [:= :products.id product-id])
      (hs/format)))


(defn get-product-by-name!
  [bucket-id name]
  (->> (get-product-by-name-query bucket-id name)
       (jdbc/query db/db)
       (first)))


(defn get-state-for-product-detail!
  [bucket-id id]
  (->> (get-product-detail-state-by-id-query bucket-id id)
       (jdbc/query db/db)
       (first)))
