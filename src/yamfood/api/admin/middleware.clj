(ns yamfood.api.admin.middleware
  (:require
    [yamfood.core.admin.core :as adm]))


(defn wrap-auth
  [handler]
  (fn [request]
    (let [token (get (:headers request) "token" "")
          admin (adm/admin-by-token! token)
          request (assoc request :admin admin)]
      (cond
        (not token) {:body {:error "Auth required"} :status 401}
        (not admin) {:body {:error "Auth failed"} :status 403}
        admin (handler request)))))

