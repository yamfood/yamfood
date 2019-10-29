(ns yamfood.core.products.core
  (:require [yamfood.core.db.core :as db]
            [honeysql.core :as hs]
            [clojure.java.jdbc :as jdbc]))


(defn get-all-products-query []
  (hs/format {:select [:id :name :price :photo :energy]
              :from   [:products]}))


(defn get-all-products! []
  (->> (get-all-products-query)
       (jdbc/query db/db)))






