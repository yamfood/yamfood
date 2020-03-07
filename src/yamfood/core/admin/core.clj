(ns yamfood.core.admin.core
  (:require
    [honeysql.core :as hs]
    [clojure.string :as str]
    [honeysql.helpers :as hh]
    [clojure.java.jdbc :as jdbc]
    [yamfood.utils :refer [uuid]]
    [yamfood.core.db.core :as db]))


(def admin-query
  {:select [:admins.id
            :admins.login
            :admins.password
            :admins.token
            :admins.payload]
   :from   [:admins]
   :where  [:= :admins.is_active true]})


(defn admin-by-credentials-query
  [login password]
  (hh/merge-where
    admin-query
    [:and
     [:= :admins.login login]
     [:= :admins.password password]]))


(defn admin-by-credentials!
  [login password]
  (->> (admin-by-credentials-query login password)
       (hs/format)
       (jdbc/query db/db)
       (first)))


(defn admin-by-login!
  [login]
  (->> (-> admin-query
           (hh/merge-where [:= :admins.login login]))
       (hs/format)
       (jdbc/query db/db)
       (first)))


(defn admin-by-token-query
  [token]
  (hh/merge-where
    admin-query
    [:= :admins.token token]))


(defn admin-by-token!
  [token]
  (->> (hs/format (admin-by-token-query token))
       (jdbc/query db/db)
       (first)))


(defn update-admin-token!
  [admin-id]
  (let [token (str/replace (uuid) "-" "")]
    (jdbc/update!
      db/db "admins"
      {:token token}
      ["admins.id = ?" admin-id])
    token))


(defn update-admin!
  [admin-id row]
  (jdbc/update! db/db "admins" row ["admins.id = ?" admin-id]))


(defn all-admins!
  []
  (->> admin-query
       (hs/format)
       (jdbc/query db/db)))


(defn create-admin!
  [admin]
  (-> (jdbc/insert! db/db "admins" admin)
      (first)))


(defn delete-admin!
  [admin-id]
  (jdbc/update!
    db/db "admins"
    {:is_active false}
    ["admins.id = ?" admin-id]))
