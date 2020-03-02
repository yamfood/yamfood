(ns yamfood.api.admin.handlers.admins
  (:require
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [yamfood.core.admin.core :as a]))


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


(s/def ::login string?)
(s/def ::password string?)
(s/def ::payload map?)
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


(c/defroutes
  routes
  (c/GET "/" [] admins-list)
  (c/POST "/" [] create-admin)
  (c/GET "/permissions/" [] permissions-list))

