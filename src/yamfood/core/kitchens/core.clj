(ns yamfood.core.kitchens.core
  (:require
    [yamfood.utils :as u]
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [clojure.java.jdbc :as jdbc]
    [clj-postgresql.core :as pg]
    [yamfood.core.db.core :as db]))


(def kitchen-query
  {:select   [:kitchens.id
              :kitchens.name
              :kitchens.location
              :kitchens.start_at
              :kitchens.end_at
              :kitchens.payload
              :kitchens.is_disabled]
   :from     [:kitchens]
   :order-by [:kitchens.id]})


(def open-kitchens-where
  [:case
   [:> :kitchens.start_at :kitchens.end_at]
   [(hs/raw "now()::time + interval '5 hours' not between kitchens.end_at and kitchens.start_at")]
   [:< :kitchens.start_at :kitchens.end_at]
   [(hs/raw "now()::time + interval '5 hours' between kitchens.start_at and kitchens.end_at")]])


(defn fmt-kitchen
  [kitchen]
  (-> kitchen
      (update :start_at u/->time)
      (update :end_at u/->time)
      (update :location db/point->clj)))


(defn all-kitchens!
  []
  (->> kitchen-query
       (hs/format)
       (jdbc/query db/db)
       (map fmt-kitchen)))


(defn kitchen-by-id!
  [kitchen-id]
  (->> (-> kitchen-query
           (hh/merge-where [:= :kitchens.id kitchen-id])
           (hs/format))
       (jdbc/query db/db)
       (map fmt-kitchen)
       (first)))


(def kitchens-distance-function-query
  "ST_Distance(geometry(kitchens.location), geometry(point(%s, %s)))")


(defn nearest-kitchen-query
  [lon lat]
  (-> (assoc
        kitchen-query
        :order-by
        [(hs/raw
           (format
             kitchens-distance-function-query
             lon lat))])
      (hh/merge-where open-kitchens-where)))


(defn nearest-kitchen!
  [lon lat]
  (->> (nearest-kitchen-query lon lat)
       (hs/format)
       (jdbc/query db/db)
       (map fmt-kitchen)
       (first)))


(def disabled-products-query
  {:select [:products.id
            :products.thumbnail
            :products.name]
   :from   [:disabled_products :products]
   :where  [:= :disabled_products.product_id :products.id]})


(defn kitchen-disabled-products!
  [kitchen-id]
  (->> (-> disabled-products-query
           (hh/merge-where [:= :disabled_products.kitchen_id kitchen-id])
           (hs/format))
       (jdbc/query db/db)))


(defn add-disabled-product!
  [kitchen-id product-id]
  (jdbc/insert!
    db/db
    "disabled_products"
    {:kitchen_id kitchen-id :product_id product-id}))


(defn delete-disabled-product!
  [kitchen-id product-id]
  (jdbc/delete!
    db/db
    "disabled_products"
    ["kitchen_id = ? and product_id = ?" kitchen-id product-id]))


(defn create!
  [name lon lat payload start_at end_at]
  (->> (jdbc/insert!
         db/db
         "kitchens"
         {:name     name :location (pg/point lon lat) :payload payload
          :start_at (u/time->sql start_at) :end_at (u/time->sql end_at)})
       (map fmt-kitchen)
       (first)))


(defn prepare-for-update
  [kitchen]
  (let [location (:location kitchen)
        lon (:longitude location)
        lat (:latitude location)]
    (-> kitchen
        (assoc :location (pg/point lon lat))
        (update :start_at u/time->sql)
        (update :end_at u/time->sql))))


(defn update!
  [kitchen-id kitchen]
  (let [kitchen (prepare-for-update kitchen)]
    (jdbc/update!
      db/db
      "kitchens"
      kitchen
      ["kitchens.id = ?" kitchen-id])))
