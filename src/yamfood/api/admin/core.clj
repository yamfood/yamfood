(ns yamfood.api.admin.core
  (:require
    [compojure.core :as c]
    [yamfood.api.admin.handlers.auth :as auth]
    [yamfood.api.admin.handlers.users :as users]
    [yamfood.api.admin.handlers.orders :as orders]
    [yamfood.api.admin.middleware :refer [wrap-auth]]
    [yamfood.api.admin.handlers.products :as products]))


(c/defroutes
  admin-api-routes
  (c/context "/auth" [] auth/auth-routes)
  (wrap-auth
    (c/routes
      (c/context "/users" [] users/user-routes)
      (c/context "/orders" [] orders/orders-routes)
      (c/context "/products" [] products/products-routes))))

