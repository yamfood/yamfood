(ns yamfood.core.users.core
  (:require [yamfood.core.db.core :as db]
            [honeysql.core :as hs]
            [clojure.java.jdbc :as jdbc]
            [honeysql.helpers :as hh]))


(def user-query
  {:select [:users.id
            :users.phone
            :users.tid
            :users.location
            :users.comment
            [:baskets.id :basket_id]]
   :from   [:users :baskets]
   :where  [:= :baskets.user_id :users.id]})


(defn location->clj
  [user]
  (let [point (:location user)]
    (assoc user :location (db/point->clj point))))


(defn user-with-tid-query
  [tid]
  (hs/format (hh/merge-where user-query [:= :users.tid tid])))


(defn user-with-basket-id-query
  [basket-id]
  (hs/format (hh/merge-where user-query [:= :baskets.id basket-id])))


(defn user-with-basket-id!
  [basket-id]
  (->> (user-with-basket-id-query basket-id)
       (jdbc/query db/db)
       (first)
       (location->clj)))


(defn user-with-tid!
  [tid]                                                     ; TODO: CACHE!
  (let [user (->> (user-with-tid-query tid)
                  (jdbc/query db/db)
                  (first))]
    (if user
        (location->clj user)
        nil)))


(defn insert-user!
  [tid phone]
  (first (jdbc/insert! db/db "users" {:tid tid :phone phone})))


(defn init-basket!
  [user-id]
  (jdbc/insert! db/db "baskets" {:user_id user-id}))


(defn create-user!
  [tid phone]
  (let [user (insert-user! tid phone)]
    (init-basket! (:id user))))


(defn update-location-query
  [user-id lon lat]
  (hs/format {:update :users
              :set    {:location (hs/raw (format "POINT(%s, %s)" lon lat))}
              :where  [:= :id user-id]}))


(defn update-location!
  "Lon - longitude (X)
   Lat - latitude (Y)"
  [user-id lon lat]
  (->> (update-location-query user-id lon lat)
       (jdbc/execute! db/db)))


(defn update-comment!
  [user-id comment]
  (jdbc/update! db/db "users" {:comment comment} ["id = ?" user-id]))