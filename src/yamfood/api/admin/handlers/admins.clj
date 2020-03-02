(ns yamfood.api.admin.handlers.admins
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [yamfood.core.admin.core :as a]))


(s/def ::login string?)
(s/def ::password string?)
(s/def ::payload map?)


(def permissions
  {:can-see-products "can_see_products"
   :can-see-clients  "can_see_clients"
   :can-see-riders   "can_see_riders"})


(defn permissions-list
  [_]
  {:body (map #(second %) (seq permissions))})


(defn admins-list
  [_]
  {:body (a/all-admins!)})


(s/def ::admin-create
  (s/keys :req-un [::login ::password]
          :opt-un [::payload]))


(defn create-admin
  [request]
  (let [body (:body request)
        valid? (s/valid? ::admin-create body)]
    (if valid?
      {:body (a/create-admin! body)}
      {:body   {:error "Incorrect input"}
       :status 400})))


(s/def ::admin-patch
  (s/keys :opt-un [::login ::password ::payload]))


(defn patch-admin
  [request]
  (let [admin-id (u/str->int (:id (:params request)))
        body (:body request)
        valid? (s/valid? ::admin-patch body)]
    (if valid?
      (do
        (a/update-admin! admin-id body)
        {:body body})
      {:body   {:error "Invalid input"}
       :status 400})))


(c/defroutes
  routes
  (c/GET "/" [] admins-list)
  (c/POST "/" [] create-admin)
  (c/PATCH "/:id{[0-9]+}/" [] patch-admin)
  (c/GET "/permissions/" [] permissions-list))

