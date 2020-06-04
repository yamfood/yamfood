(ns user
  (:require
    [yamfood.nrepl]
    [yamfood.core.db.core]
    [yamfood.core]
    [yamfood.tasks.core]
    [mount.core :as mount]
    [environ.core :refer [env]]
    [migratus.core :as migratus]
    [yamfood.core.db.migrations :as m]))


(defn start []
  (mount/start-without #'yamfood.nrepl/server))


(defn stop []
  (mount/stop-except #'yamfood.nrepl/server))


(defn restart []
  (stop)
  (start))


(defn migrate []
  (migratus/migrate m/config))


(defn rollback []
  (migratus/rollback m/config))


(defn create-migration [name]
  (migratus/create m/config name))


#_(start)
