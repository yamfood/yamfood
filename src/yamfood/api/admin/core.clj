(ns yamfood.api.admin.core
  (:require
    [compojure.core :as c]
    [yamfood.api.admin.handlers.auth :as auth]
    [yamfood.api.admin.handlers.users :as users]
    [yamfood.api.admin.handlers.riders :as riders]
    [yamfood.api.admin.handlers.orders :as orders]
    [yamfood.api.admin.middleware :refer [wrap-auth]]
    [yamfood.api.admin.handlers.products :as products]))


(c/defroutes
  routes
  (c/context "/auth" [] auth/routes)
  (wrap-auth
    (c/routes
      (c/context "/users" [] users/routes)
      (c/context "/riders" [] riders/routes)
      (c/context "/orders" [] orders/routes)
      (c/context "/products" [] products/routes))))

