(ns yamfood.core.db.init
  (:require
    [migratus.core :as migratus]
    [yamfood.core.db.migrations :as m]
    [yamfood.core.db.core :refer [db]]
    [mount.core :as mount]))


(defn init []
  (mount/start #'db)
  (migratus/migrate (m/config)))


;(init)
;(migratus/create (m/config) "rider_deposits_timestamps")
