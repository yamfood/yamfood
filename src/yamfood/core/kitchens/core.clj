(ns yamfood.core.kitchens.core
  (:require
    [honeysql.core :as hs]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]))


(def kitchen-query
  {:select   [:kitchens.id
              :kitchens.name
              :kitchens.location
              :kitchens.payload]
   :from     [:kitchens]
   :order-by [:kitchens.id]})


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


(def kitchens-distance-function-query
  "ST_Distance(geometry(kitchens.location), geometry(point(%s, %s)))")


(defn nearest-kitchen-query
  [lon lat]
  (assoc
    kitchen-query
    :order-by
    [(hs/raw
       (format
         kitchens-distance-function-query
         lon lat))]))


(defn nearest-kitchen!
  [lon lat]
  (->> (nearest-kitchen-query lon lat)
       (hs/format)
       (jdbc/query db/db)
       (first)
       (fmt-location)))
