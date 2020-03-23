(ns yamfood.core.kitchens.core
  (:require
    [honeysql.core :as hs]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [honeysql.helpers :as hh]
    [clj-postgresql.core :as pg]))


(def kitchen-query
  {:select   [:kitchens.id
              :kitchens.name
              :kitchens.location
              :kitchens.start_at
              :kitchens.end_at
              :kitchens.payload]
   :from     [:kitchens]
   :order-by [:kitchens.id]})


(def open-kitchens-where
  [:case
   [:> :kitchens.start_at :kitchens.end_at]
   [(hs/raw "now()::time + interval '5 hours' not between kitchens.end_at and kitchens.start_at")]
   [:< :kitchens.start_at :kitchens.end_at]
   [(hs/raw "now()::time + interval '5 hours' between kitchens.start_at and kitchens.end_at")]])


(defn fmt-location
  [kitchen]
  (let [pg-location (:location kitchen)]
    (assoc kitchen :location (db/point->clj pg-location))))


(defn all-kitchens!
  []
  (->> kitchen-query
       (hs/format)
       (jdbc/query db/db)
       (map fmt-location)))


(defn kitchen-by-id!
  [kitchen-id]
  (->> (-> kitchen-query
           (hh/merge-where [:= :kitchens.id kitchen-id])
           (hs/format))
       (jdbc/query db/db)
       (map fmt-location)
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
       (map fmt-location)
       (first)))


(defn create!
  [name lon lat]
  (->> (jdbc/insert!
         db/db
         "kitchens"
         {:name name :location (pg/point lon lat)})
       (map fmt-location)
       (first)))
