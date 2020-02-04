(ns yamfood.api.admin.users
  (:require
    [yamfood.core.users.core :as users]
    [compojure.core :as c]))


(defn users-list
  [request]
  {:body (users/users-list!)})


(c/defroutes
  user-routes
  (c/GET "/" [] users-list))