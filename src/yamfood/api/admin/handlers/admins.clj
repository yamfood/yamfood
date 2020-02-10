(ns yamfood.api.admin.handlers.admins
  (:require
    [compojure.core :as c]
    [yamfood.core.admin.core :as a]))


(defn admins-list
  [_]
  {:body (a/all-admins!)})

(c/defroutes
  routes
  (c/GET "/" [] admins-list))
