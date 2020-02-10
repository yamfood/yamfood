(ns yamfood.api.admin.handlers.riders
  (:require
    [compojure.core :as c]
    [yamfood.core.riders.core :as r]))


(defn riders-list
  [_]
  {:body (r/all-riders!)})


(c/defroutes
  routes
  (c/GET "/" [] riders-list))
