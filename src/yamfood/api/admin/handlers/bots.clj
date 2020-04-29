(ns yamfood.api.admin.handlers.bots
  (:require
    [compojure.core :as c]
    [yamfood.core.bots.core :as b]))


(defn bots-list
  [_]
  {:body (b/all-bots!)})


(c/defroutes
  routes
  (c/GET "/" [] bots-list))

