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


(defn product-detail-query
  [bucket-id]
  {:select   [:products.id :products.name :products.price
              :products.photo :products.thumbnail
              :products.energy
              (hs/raw (format "(select count(id) from bucket_products where bucket_id = %d) as positions_in_bucket" bucket-id))
              (hs/raw "coalesce(bucket_products.count, 0) as count_in_bucket")]
   :from     [:products]
   :order-by [:id]
   :left-join [:bucket_products [:and
                                 [:= :bucket_products.bucket_id bucket-id]
                                 [:= :products.id :bucket_products.product_id]]]
   :limit    1})

(defn- get-product-by-name-query
  [bucket-id name]
  (-> (product-detail-query bucket-id)
    (hh/merge-where [:= :products.name name])
    (hs/format)))


(defn- get-product-by-id-query
  [bucket-id product-id]
  (-> (product-detail-query bucket-id)
      (hh/merge-where [:= :products.id product-id])
      (hs/format)))


(defn get-product-by-name!
  [bucket-id name]
  (->> (get-product-by-name-query bucket-id name)
       (jdbc/query db/db)
       (first)))


(defn get-state-for-product-detail!
  [bucket-id id]
  (->> (get-product-by-id-query bucket-id id)
       (jdbc/query db/db)
       (first)))
