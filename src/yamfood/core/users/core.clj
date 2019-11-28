(ns yamfood.core.users.core
  (:require [yamfood.core.db.core :as db]
            [honeysql.core :as hs]
            [clojure.java.jdbc :as jdbc]))


(defn get-user-by-tid-query
  [tid]
  (hs/format {:select [:users.id :users.phone :users.tid [:baskets.id :basket_id]]
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
