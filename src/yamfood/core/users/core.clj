(ns yamfood.core.users.core
  (:require [yamfood.core.db.core :as db]
            [honeysql.core :as hs]
            [clojure.java.jdbc :as jdbc]))


(defn get-user-by-tid-query
  [tid]
  (hs/format {:select [:users.id
                       :users.phone
                       :users.tid
                       :users.location
                       [:baskets.id :basket_id]]
              :from   [:users :baskets]
              :where  [:and
                       [:= :users.tid tid]
                       [:= :baskets.user_id :users.id]]}))


(defn get-user-by-tid!
  [tid]                                                     ; TODO: CACHE!
  (->> (get-user-by-tid-query tid)
       (jdbc/query db/db)
       (first)))

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
              :set {:location (hs/raw (format "POINT(%s, %s)" lon lat))}
              :where [:= :id user-id]}))


(defn update-location!
  "Lon - longitude (X)
   Lat - latitude (Y)"
  [user-id lon lat]
  (->> (update-location-query user-id lon lat)
       (jdbc/execute! db/db)))

