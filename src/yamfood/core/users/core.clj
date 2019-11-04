(ns yamfood.core.users.core
  (:require [yamfood.core.db.core :as db]
            [honeysql.core :as hs]
            [clojure.java.jdbc :as jdbc]))


(defn get-user-by-tid-query
  [tid]
  (hs/format {:select [:id :phone :tid]
              :from   [:users]
              :where  [:= :tid tid]}))


(defn get-user-by-tid!
  [tid]                                                     ; TODO: CACHE!
  (->> (get-user-by-tid-query tid)
       (jdbc/query db/db)
       (first)))


(defn create-user!
  [tid phone]
  (first (jdbc/insert! db/db "users" {:tid tid :phone phone})))
