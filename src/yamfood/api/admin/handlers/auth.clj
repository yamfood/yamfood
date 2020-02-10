(ns yamfood.api.admin.handlers.auth
  (:require
    [compojure.core :as c]
    [compojure.route :as route]
    [yamfood.core.admin.core :as adm]))


(defn login
  [request]
  (let [body (:body request)
        login (:login body)
        password (:password body)
        admin (adm/admin-by-credentials! login password)]
    (if admin
      (let [token (adm/update-admin-token! (:id admin))]
        {:body {:token token}})
      {:body   {:request (str request)
                :error   "Incorrect credentials"}
       :status 403})))


(c/defroutes
  routes
  (c/POST "/login/" [] login))
