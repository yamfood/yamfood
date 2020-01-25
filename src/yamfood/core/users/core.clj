(ns yamfood.core.users.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]))


(defn user-active-order-query
  [user-id]
  {:select   [:orders.id]
   :from     [:orders :order_logs]
   :where    [:and
              [:= :orders.user_id user-id]
              [:= :orders.id :order_logs.order_id]
              [:= :order_logs.status "new"]]
   :order-by [[:orders.created_at :desc]]
   :limit    1})


(def user-query
  {:select [:users.id
            :users.phone
            :users.tid
            :users.location
            :users.comment
            [(user-active-order-query :users.id) :active_order_id]
            [:baskets.id :basket_id]]
   :from   [:users :baskets]
   :where  [:= :baskets.user_id :users.id]})


(defn location->clj
  [user]
  (let [point (:location user)]
    (if point
      (assoc user :location (db/point->clj point))
      user)))


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
  [tid phone name]
  (first (jdbc/insert! db/db "users" {:tid tid :phone phone :name name})))


(defn init-basket!
  [user-id]
  (jdbc/insert! db/db "baskets" {:user_id user-id}))


(defn create-user!
  [tid phone name]
  (let [user (insert-user! tid phone name)]
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
