(ns yamfood.api.admin.handlers.users
  (:require
    [compojure.core :as c]
    [yamfood.core.users.core :as users]))


(defn users-list
  [request]
  {:body (users/users-list!)})


(c/defroutes
  routes
  (c/GET "/" [] users-list))