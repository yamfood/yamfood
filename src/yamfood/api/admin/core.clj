(ns yamfood.api.admin.core
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [clojure.string :as str]
    [yamfood.integrations.s3.core :as s3]
    [yamfood.api.admin.handlers.ws :as ws]
    [yamfood.api.admin.handlers.auth :as auth]
    [yamfood.api.admin.handlers.data :as data]
    [yamfood.api.admin.handlers.bots :as bots]
    [yamfood.api.admin.handlers.riders :as riders]
    [yamfood.api.admin.handlers.admins :as admins]
    [yamfood.api.admin.handlers.orders :as orders]
    [yamfood.api.admin.handlers.params :as params]
    [yamfood.api.admin.handlers.clients :as clients]
    [yamfood.api.admin.middleware :refer [wrap-auth]]
    [yamfood.api.admin.handlers.kitchens :as kitchens]
    [yamfood.api.admin.handlers.products :as products]
    [yamfood.api.admin.handlers.announcements :as announcements]))


(defn sign-s3
  [request]
  (let [params (:params request)
        folder (get params "folder" "default")
        file-name (get params "file-name")
        file-type (last (str/split file-name #"\."))
        file-name (str (u/uuid) "." file-type)
        key (str folder "/" file-name)]
    {}
    {:body {:url           (format "https://s3-eu-west-1.amazonaws.com/%s/%s"
                                   s3/bucket
                                   key)
            :signedRequest (str (s3/generate-presigned-url! key))}}))


(c/defroutes
  routes
  (c/context "/auth" [] auth/routes)
  (c/context "/ws" [] ws/routes)
  (wrap-auth
    (c/routes
      (c/GET "/sign-s3" [] sign-s3)
      (c/context "/data" [] data/routes)
      (c/context "/bots" [] bots/routes)
      (c/context "/admins" [] admins/routes)
      (c/context "/riders" [] riders/routes)
      (c/context "/orders" [] orders/routes)
      (c/context "/params" [] params/routes)
      (c/context "/clients" [] clients/routes)
      (c/context "/kitchens" [] kitchens/routes)
      (c/context "/products" [] products/routes)
      (c/context "/announcements" [] announcements/routes))))

