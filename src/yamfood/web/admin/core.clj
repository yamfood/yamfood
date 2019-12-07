(ns yamfood.web.admin.core
  (:require [compojure.core :as c]
            [compojure.route :as route]
            [yamfood.web.admin.handlers.login :as login]
            [yamfood.web.admin.handlers.dashboard :as dashboard]))


(c/defroutes
  routes
  (c/GET "/login" [] login/login-handler!)
  (c/GET "/dashboard" [] dashboard/dashboard-handler!)
  (route/not-found "Not found"))


