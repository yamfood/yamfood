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
            :users.payload
            [(user-active-order-query :users.id) :active_order_id]
            [:baskets.id :basket_id]]
   :from   [:users :baskets]
   :where  [:= :baskets.user_id :users.id]})


(defn keywordize
  [data]
  (into
    {}
    (for [[k v] data]
      [(keyword k) (if (map? v) (keywordize v) v)])))


(defn fmt-payload
  [user]
  (let [payload (:payload user)]
    (assoc user :payload (keywordize payload))))


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
       (map fmt-payload)
       (first)))


(defn user-with-tid!
  [tid]                                                     ; TODO: CACHE!
  (->> (user-with-tid-query tid)
       (jdbc/query db/db)
       (map fmt-payload)
       (first)))


(defn users-list-query
  [offset limit]
  (hs/format
    {:select [:users.id
              :users.tid
              :users.comment
              :users.name
              :users.phone
              :users.location]
     :from   [:users]
     :offset offset
     :limit  limit}))


(defn users-list!
  ([]
   (users-list! 0 100))
  ([offset limit]
   (->> (users-list-query offset limit)
        (jdbc/query db/db))))


(defn insert-user!
  [tid name]
  (first (jdbc/insert! db/db "users" {:tid tid :name name})))


(defn init-basket!
  [user-id]
  (jdbc/insert! db/db "baskets" {:user_id user-id}))


(defn create-user!
  [tid name]
  (let [user (insert-user! tid name)]
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


(defn update-phone!
  [user-id phone]
  (jdbc/update! db/db "users" {:phone phone} ["id = ?" user-id]))


(defn update-payload!
  [user-id payload]
  (jdbc/update! db/db "users" {:payload payload} ["id = ?" user-id]))
