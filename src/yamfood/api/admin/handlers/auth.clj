(ns yamfood.api.admin.handlers.auth
  (:require
    [compojure.core :as c]
    [yamfood.core.admin.core :as a]))


(defn login
  [request]
  (let [body (:body request)
        login (:login body)
        password (:password body)
        admin (a/admin-by-credentials! login password)]
    (if admin
      (let [token (a/update-admin-token! (:id admin))]
        {:body {:token token}})
      {:body   {:request (str request)
                :error   "Incorrect credentials"}
       :status 403})))


(c/defroutes
  routes
  (c/POST "/login/" [] login))
