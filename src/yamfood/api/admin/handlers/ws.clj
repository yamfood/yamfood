(ns yamfood.api.admin.handlers.ws
  (:require
    [compojure.core :as c]
    [yamfood.api.admin.handlers.orders :as orders]))


(c/defroutes
  routes
  (c/GET "/order/" [] orders/ws-handler))