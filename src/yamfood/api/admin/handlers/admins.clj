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


(defn validate-create-admin!
  [body]
  (let [valid? (s/valid? ::admin-create body)]
    (if valid?
      (nil? (a/admin-by-login! (:login body)))
      false)))


(defn create-admin
  [request]
  (let [body (:body request)
        valid? (validate-create-admin! body)]
    (if valid?
      {:body (a/create-admin! body)}
      {:body   {:error "Incorrect input"}
       :status 400})))


(s/def ::admin-patch
  (s/keys :opt-un [::login ::password ::payload]))


(defn validate-patch-admin!
  [body admin-id]
  (let [valid? (s/valid? ::admin-patch body)]
    (if valid?
      (let [admin (a/admin-by-login! (:login body))]
        (or (nil? admin) (= (:id admin) admin-id)))
      false)))


(defn patch-admin
  [request]
  (let [admin-id (u/str->int (:id (:params request)))
        body (:body request)
        valid? (validate-patch-admin! body admin-id)]
    (if valid?
      (do
        (a/update-admin! admin-id body)
        {:body body})
      {:body   {:error "Invalid input"}
       :status 400})))


(defn delete-admin
  [request]
  (let [admin-id (u/str->int (:id (:params request)))
        result (a/delete-admin! admin-id)]
    (if result
      {:body {:result "ok"}}
      {:body   {:error "Not deleted"}
       :status 400})))


(c/defroutes
  routes
  (c/GET "/" [] admins-list)
  (c/POST "/" [] create-admin)
  (c/PATCH "/:id{[0-9]+}/" [] patch-admin)
  (c/DELETE "/:id{[0-9]+}/" [] delete-admin)
  (c/GET "/permissions/" [] permissions-list))

