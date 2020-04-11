(ns yamfood.api.admin.handlers.orders
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [yamfood.api.pagination :as p]
    [yamfood.core.orders.core :as o]
    [yamfood.core.orders.core :as ord]
    [yamfood.integrations.iiko.core :as iiko]
    [yamfood.telegram.helpers.status :as status]
    [clojure.data.json :as json]))


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


(defn accept-order
  [request]
  (let [admin-id (:id (:admin request))
        order-id (u/str->int (:id (:params request)))
        order (o/order-by-id! order-id)
        acceptable? (= (:new o/order-statuses) (:status order))]
    (if acceptable?
      (do
        (if (do (o/accept-order! (:id order) admin-id)
                (status/notify-order-accepted! (:id order)))
          {:body (get-active-orders!)}
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Can't accept order in this status"}
       :status 400})))


(defn cancel-order
  [request]
  (let [order-id (u/str->int (:id (:params request)))
        order (o/order-by-id! order-id)
        cancelable? (u/in? o/cancelable-order-statuses
                           (:status order))]
    (if cancelable?
      (do
        (if (do (o/cancel-order! (:id order))
                (status/notify-order-canceled! (:id order)))
          {:body (get-active-orders!)}
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Can't cancel order in this status"}
       :status 400})))


(defn finished-orders-where
  [order-id client-phone rider-phone]
  (if (every? nil? [order-id client-phone rider-phone])
    nil
    (remove
      nil?
      [:and
       (when order-id [:= :cte_orders.id order-id])
       (when client-phone [:= :cte_orders.phone client-phone])
       (when rider-phone [:= :cte_orders.rider_phone rider-phone])])))


(defn finished-orders
  [request]
  (let [page (p/get-page request)
        per-page (p/get-per-page request)
        offset (p/calc-offset page per-page)
        params (:params request)
        order-id (u/str->int (get params "order_id"))
        client-phone (u/str->int (get params "client_phone"))
        rider-phone (u/str->int (get params "rider_phone"))
        where (finished-orders-where order-id
                                     client-phone
                                     rider-phone)
        count (o/ended-orders-count! where)]
    {:body (p/format-result
             count
             per-page
             page
             (o/ended-orders!
               offset
               per-page
               where))}))


(c/defroutes
  routes
  (c/GET "/:id{[0-9]+}/" [] order-details)
  (c/POST "/:id{[0-9]+}/accept/" [] accept-order)
  (c/POST "/:id{[0-9]+}/cancel/" [] cancel-order)

  (c/GET "/active/" [] active-orders-list)
  (c/GET "/finished/" [] finished-orders))


;(def order (o/order-by-id! 6))
;(iiko/check-order! (iiko/access-token!)
;                   (iiko/order->iiko order))
;(iiko/create-order! order)
;(json/write-str (iiko/order->iiko order))
;(iiko/order-info! (iiko/access-token!)
;                  "ec70bdd7-d33c-4cf1-b865-cb77659d37ca")
