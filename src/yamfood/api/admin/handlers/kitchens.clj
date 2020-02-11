(ns yamfood.api.admin.handlers.kitchens
  (:require
    [compojure.core :as c]
    [yamfood.core.kitchens.core :as k]))


(defn kitchens-list
  [_]
  {:body (k/all-kitchens!)})


(c/defroutes
  routes
  (c/GET "/" [] kitchens-list))

