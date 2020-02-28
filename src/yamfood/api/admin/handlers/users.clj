(ns yamfood.api.admin.handlers.users
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [yamfood.api.pagination :as p]
    [yamfood.core.users.core :as users]
    [clojure.spec.alpha :as s]))


(defn fmt-user-details
  [user]
  (map
    #(hash-map :label (str (first %))
               :value (str (second %)))
    (seq user)))


(defn user-detail
  [request]
  (let [user-id (u/str->int (:id (:params request)))]
    {:body (-> (users/user-with-id! user-id)
               (fmt-user-details))}))


(s/def ::is_blocked boolean?)
(s/def ::user-patch
  (s/keys :opt-un [::is_blocked]))

(defn user-patch
  [request]
  (let [user-id (u/str->int (:id (:params request)))
        data (:body request)
        valid? (s/valid? ::user-patch data)]
    (if valid?
      (do
        (users/update! user-id data)
        {:body data})
      {:body "Invalid input"
       :code 400})))


(defn users-list
  [request]
  (let [page (p/get-page request)
        per-page (p/get-per-page request)
        phone (u/str->int (get (:params request) "phone" ""))
        search (when phone [:= :users.phone phone])
        count (users/users-count! search)
        offset (p/calc-offset page per-page)]
    {:body (p/format-result
             count
             per-page
             page
             (users/users-list!
               offset
               per-page
               search))}))


(c/defroutes
  routes
  (c/GET "/" [] users-list)
  (c/GET "/:id{[0-9]+}/" [] user-detail)
  (c/PATCH "/:id{[0-9]+}/" [] user-patch))
