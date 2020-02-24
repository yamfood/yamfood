(ns yamfood.api.admin.handlers.orders
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [yamfood.core.orders.core :as o]
    [yamfood.core.orders.core :as ord]))


(defn reduce-active-orders
  [result order]
  (let [status (keyword (:status order))
        prev (status result)]
    (assoc result status (into prev [order]))))


(defn get-active-orders!
  []
  (reduce
    reduce-active-orders
    (apply
      merge
      (map #(hash-map (keyword %) [])
           ord/active-order-statuses))
    (ord/active-orders!)))


(defn active-orders-list
  [_]
  {:body (get-active-orders!)})


(defn order-details
  [request]
  (let [order-id (u/str->int (:id (:params request)))]
    {:body (o/order-by-id! order-id)}))

(defn cancel-order
  [request]
  (let [order-id (u/str->int (:id (:params request)))
        order (o/order-by-id! order-id)
        cancelable (u/in? o/cancelable-order-statuses
                          (:status order))]
    (if cancelable
      (do
        (if (o/cancel-order! (:id order))
          {:body (get-active-orders!)}
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Can't cancel order in this status"}
       :status 400})))


(defn finished-orders                                       ; TODO: Use pagination!!!
  [_]
  {:body (o/finished-orders!)})


(c/defroutes
  routes
  (c/GET "/:id{[0-9]+}/" [] order-details)
  (c/POST "/:id{[0-9]+}/cancel/" [] cancel-order)

  (c/GET "/active/" [] active-orders-list)
  (c/GET "/finished/" [] finished-orders))
