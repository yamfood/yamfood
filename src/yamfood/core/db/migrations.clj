(ns yamfood.core.db.migrations
  (:require
    [yamfood.core.db.core :as db]))


(defn config []
  {:store                :database
   :migration-dir        "migrations/"
   :init-in-transaction? false
   :migration-table-name "migrations"
   :db                   db/db})
