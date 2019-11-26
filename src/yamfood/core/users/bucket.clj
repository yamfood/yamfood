(ns yamfood.core.users.bucket
  (:require [yamfood.core.products.core :as p]
            [yamfood.core.db.core :as db]
            [honeysql.core :as hs]
            [clojure.java.jdbc :as jdbc]
            [honeysql.helpers :as hh]))


(defn- get-bucket-query
  [user-id]
  (hs/format {:select [:id]
              :from   [:buckets]
              :where  [:= :user-id user-id]}))


(defn- get-bucket-products-query
  [bucket-id]
  (hs/format {:select [:product_id :count]
              :from   [:bucket_products]
              :where  [:= :bucket_id bucket-id]}))


(defn- get-bucket-products!
  [bucket-id]
  (->> (get-bucket-products-query bucket-id)
       (jdbc/query db/db)))


(defn- get-bucket!
  [user-id]
  (->> (get-bucket-query user-id)
       (jdbc/query db/db)
       (first)))


(defn increment-product-query
  [bucket-id product-id]
  (hs/format {:update :bucket_products
              :set {:count (hs/raw "count + 1")}
              :where [:and
                      [:= :bucket_id bucket-id]
                      [:= :product_id product-id]]}))


(defn decrement-product-query
  [bucket-id product-id]
  (hs/format {:update :bucket_products
              :set {:count (hs/raw "count - 1")}
              :where [:and
                      [:= :bucket_id bucket-id]
                      [:= :product_id product-id]]}))


(defn increment-product-in-bucket!
  [bucket-id product-id]
  (->> (increment-product-query bucket-id product-id)
      (jdbc/execute! db/db))
  (p/get-state-for-product-detail! bucket-id product-id))


(defn decrement-product-in-bucket!
  [bucket-id product-id]
  (->> (decrement-product-query bucket-id product-id)
       (jdbc/execute! db/db))
  (p/get-state-for-product-detail! bucket-id product-id))


(defn- insert-product-to-bucket!
  [bucket-id product-id]
  (first
    (jdbc/insert! db/db "bucket_products"
                  {:product_id product-id
                   :bucket_id  bucket-id})))


(defn add-product-to-bucket!
  [bucket-id product-id]
  (let [product-id (:product_id (insert-product-to-bucket!
                                  bucket-id
                                  product-id))]
    (p/get-state-for-product-detail! bucket-id product-id)))


;(increment-product-in-bucket! 3 5)
;(p/get-product-by-id! 3 5)