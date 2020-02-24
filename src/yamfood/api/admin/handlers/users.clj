(ns yamfood.api.admin.handlers.users
  (:require
    [compojure.core :as c]
    [yamfood.api.pagination :as p]
    [yamfood.core.users.core :as users]))


(defn users-list
  [request]
  (let [page (p/get-page request)
        per-page (p/get-per-page request)
        count (users/users-count!)
        offset (p/calc-offset page per-page)]
    {:body (p/format-result
             count
             per-page
             page
             (users/users-list! offset per-page))}))


(c/defroutes
  routes
  (c/GET "/" [] users-list))