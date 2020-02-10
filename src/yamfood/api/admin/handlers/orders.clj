(ns yamfood.api.admin.handlers.orders
  (:require
    [compojure.core :as c]
    [yamfood.core.orders.core :as ord]))


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


(c/defroutes
  routes
  (c/GET "/active/" [] active-orders-list))
