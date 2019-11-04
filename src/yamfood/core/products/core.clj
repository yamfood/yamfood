(ns yamfood.core.products.core
  (:require [yamfood.core.db.core :as db]
            [honeysql.core :as hs]
            [clojure.java.jdbc :as jdbc]))


(defn get-all-products-query []
  (hs/format {:select [:id :name :price :photo :thumbnail :energy]
              :from   [:products]
              :order-by [:id]}))


(defn get-all-products! []
  (->> (get-all-products-query)
       (jdbc/query db/db)))


(defn get-product-by-name-query
  [name]
  (hs/format {:select [:id :name :price :photo :thumbnail :energy]
              :from [:products]
              :where [:= :name name]
              :order-by [:id]
              :limit 1}))


(defn get-product-by-name!
  [name]
  (->> (get-product-by-name-query name)
       (jdbc/query db/db)
       (first)))





