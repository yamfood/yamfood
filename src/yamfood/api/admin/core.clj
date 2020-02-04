(ns yamfood.api.admin.core
  (:require
    [compojure.core :as c]
    [yamfood.api.admin.users :as users]
    [yamfood.api.admin.products :as products]))



(c/defroutes
  admin-api-routes
  (c/context "/users" [] users/user-routes)
  (c/context "/products" [] products/products-routes))
