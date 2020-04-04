(ns yamfood.api.admin.core
  (:require
    [compojure.core :as c]
    [yamfood.api.admin.handlers.auth :as auth]
    [yamfood.api.admin.handlers.riders :as riders]
    [yamfood.api.admin.handlers.admins :as admins]
    [yamfood.api.admin.handlers.orders :as orders]
    [yamfood.api.admin.handlers.clients :as clients]
    [yamfood.api.admin.middleware :refer [wrap-auth]]
    [yamfood.api.admin.handlers.kitchens :as kitchens]
    [yamfood.api.admin.handlers.products :as products]
    [yamfood.api.admin.handlers.announcements :as announcements]))


(c/defroutes
  routes
  (c/context "/auth" [] auth/routes)
  (wrap-auth
    (c/routes
      (c/context "/admins" [] admins/routes)
      (c/context "/riders" [] riders/routes)
      (c/context "/orders" [] orders/routes)
      (c/context "/clients" [] clients/routes)
      (c/context "/kitchens" [] kitchens/routes)
      (c/context "/products" [] products/routes)
      (c/context "/announcements" [] announcements/routes))))

