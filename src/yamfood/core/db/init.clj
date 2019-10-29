(ns yamfood.core.db.init
  (:require [yamfood.core.db.migrations :as m]
            [migratus.core :as migratus]))

(defn init []
  (migratus/migrate m/config))


;(init)
;(migratus/create m/config "products")
