(ns yamfood.core.db.init
  (:require
    [migratus.core :as migratus]
    [yamfood.core.db.migrations :as m]))


(defn init []
  (migratus/migrate m/config))


;(init)
;(migratus/create m/config "user-location")
