(ns yamfood.api.admin.handlers.terminals
  (:require
    [compojure.core :as c]
    [yamfood.integrations.iiko.core :as iiko]))


(defn terminals-list
  [_]
  {:body
   (map #(select-keys % [:deliveryTerminalId :deliveryRestaurantName])
        (iiko/delivery-terminals!))})


(c/defroutes
  routes
  (c/GET "/" [] terminals-list))

