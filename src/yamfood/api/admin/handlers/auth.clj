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
      (try
        (a/update-admin-token! (:id admin))
        {:body (a/admin-by-credentials! login password)}
        (catch Exception e
          (println e)
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:request (str request)
                :error   "Incorrect credentials"}
       :status 403})))


(c/defroutes
  routes
  (c/POST "/login/" [] login))
