(ns yamfood.api.admin.handlers.clients
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [clojure.data.json :as json]
    [yamfood.api.pagination :as p]
    [yamfood.core.clients.core :as clients]))


(def labels
  {:id              "ID"
   :phone           "Номер телефона"
   :tid             "Telegram ID"
   :payload         "Мета"
   :is_blocked      "Заблокирован?"
   :active_order_id "ID активного заказа"
   :basket_id       "ID корзины"})


(defn fmt-client-details
  [client]
  (map
    #(hash-map :label ((first %) labels)
               :value (json/write-str (second %)))
    (seq client)))


(defn client-detail
  [request]
  (let [client-id (u/str->int (:id (:params request)))]
    {:body (-> (clients/client-with-id! client-id)
               (fmt-client-details))}))


(s/def ::is_blocked boolean?)
(s/def ::client-patch
  (s/keys :opt-un [::is_blocked]))

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
