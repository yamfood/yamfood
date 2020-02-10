(ns yamfood.api.admin.handlers.orders
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [yamfood.core.orders.core :as ord]
    [yamfood.core.orders.core :as o]))


(defn reduce-active-orders
  [result order]
  (let [status (keyword (:status order))
        prev (status result)]
    (assoc result status (into prev [order]))))


(defn active-orders-list
  [_]
  {:body
   (reduce
     reduce-active-orders
     (apply
       merge
       (map #(hash-map (keyword %) [])
            ord/active-order-statuses))
     (ord/active-orders!))})


(defn order-details
  [request]
  (let [order-id (u/parse-int (:id (:params request)))]
    {:body (o/order-by-id! order-id)}))


(c/defroutes
  routes
  (c/GET "/:id{[0-9]+}/" [] order-details)
  (c/GET "/active/" [] active-orders-list))
