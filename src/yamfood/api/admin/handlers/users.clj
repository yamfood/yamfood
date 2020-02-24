(ns yamfood.api.admin.handlers.users
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [yamfood.api.pagination :as p]
    [yamfood.core.users.core :as users]))


(defn fmt-user-details
  [user]
  (map
    #(hash-map :label (str (first %))
               :value (second %))
    (seq user)))


(defn user-detail
  [request]
  (let [user-id (u/str->int (:id (:params request)))]
    {:body (-> (users/user-with-id! user-id)
               (fmt-user-details))}))


(defn users-list
  [request]
  (let [page (p/get-page request)
        per-page (p/get-per-page request)
        count (users/users-count!)
        offset (p/calc-offset page per-page)]
    {:body (p/format-result
             count
             per-page
             page
             (users/users-list! offset per-page))}))


(c/defroutes
  routes
  (c/GET "/" [] users-list)
  (c/GET "/:id{[0-9]+}/" [] user-detail))
