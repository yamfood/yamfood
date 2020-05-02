(ns yamfood.api.admin.handlers.clients
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [yamfood.api.pagination :as p]
    [yamfood.core.specs.core :as cs]
    [yamfood.core.orders.core :as o]
    [yamfood.core.clients.core :as clients]))


(defn client-detail
  [request]
  (let [client-id (u/str->int (:id (:params request)))]
    {:body (-> (clients/client-with-id! client-id)
               (assoc :data [{:label "Количество завершенных заказов"
                              :value (o/client-finished-orders! client-id)}
                             {:label "Количество отмененных заказов"
                              :value (o/client-canceled-orders! client-id)}]))}))


(s/def ::is_blocked boolean?)
(s/def ::name string?)
(s/def ::client-patch
  (s/keys :opt-un [::is_blocked ::cs/phone ::name]))


(defn client-patch
  [request]
  (let [client-id (u/str->int (:id (:params request)))
        data (:body request)
        valid? (s/valid? ::client-patch data)]
    (if valid?
      (do
        (clients/update! client-id data)
        {:body data})
      {:body "Invalid input"
       :code 400})))


(defn clients-list
  [request]
  (let [page (p/get-page request)
        per-page (p/get-per-page request)
        phone (u/str->int (get (:params request) "phone" ""))
        search (when phone [:= :clients.phone phone])
        count (clients/clients-count! search)
        offset (p/calc-offset page per-page)]
    {:body (p/format-result
             count
             per-page
             page
             (clients/clients-list!
               offset
               per-page
               search))}))


(c/defroutes
  routes
  (c/GET "/" [] clients-list)
  (c/GET "/:id{[0-9]+}/" [] client-detail)
  (c/PATCH "/:id{[0-9]+}/" [] client-patch))
