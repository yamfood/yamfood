(ns yamfood.api.admin.handlers.orders
  (:require
    [aleph.http :as http]
    [yamfood.utils :as u]
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [manifold.stream :as stream]
    [clojure.data.json :as json]
    [yamfood.api.pagination :as p]
    [yamfood.core.admin.core :as a]
    [yamfood.core.specs.core :as cs]
    [yamfood.core.orders.core :as o]
    [yamfood.core.orders.core :as ord]
    [yamfood.core.products.core :as products]
    [yamfood.telegram.helpers.status :as status]))


(defonce open-orders (atom {}))


(defn reduce-active-orders
  [result order]
  (let [status (keyword (:status order))
        prev (status result)]
    (assoc result status (into prev [order]))))


(defn add-viewer!
  [order]
  (let [order-id (:id order)
        viewer ((keyword (str order-id)) @open-orders)]
    (assoc order :viewer viewer)))


(defn get-active-orders!
  []
  (reduce
    reduce-active-orders
    (apply
      merge
      (map #(hash-map (keyword %) [])
           ord/active-order-statuses))
    (map add-viewer! (ord/active-orders!))))


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


(def non-websocket-request
  {:status  400
   :headers {"content-type" "application/text"}
   :body    "Expected a websocket request."})


(defn message->clj
  [message]
  (json/read-str message :key-fn keyword))


(defn consumer!
  [message]
  (let [data (message->clj message)
        order-id (:order data)
        token (:token data)
        admin (a/admin-by-token! token)]
    (swap! open-orders assoc (keyword (str order-id)) (:login admin))))


(defn close-fn!
  [message]
  (fn []
    (let [data (message->clj message)
          order-id (:order data)]
      (swap! open-orders dissoc (keyword (str order-id))))))


(defn ws-handler
  [req]
  (if-let [socket (try
                    @(http/websocket-connection req)
                    (catch Exception e
                      nil))]
    (do
      (let [message @(stream/take! socket)]
        (consumer! message)
        (stream/on-closed
          socket
          (close-fn! message)))
      nil)
    non-websocket-request))


(defn order-available-products
  [request]
  (let [order-id (u/str->int (:id (:params request)))
        order (o/order-by-id! order-id)]
    (if order
      (try
        {:body (products/all-products! (:kitchen_id order))}
        (catch Exception e
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Order not found"}
       :status 404})))


(s/def ::product_id int?)
(s/def ::count int?)
(s/def ::order-products
  (s/keys :req-un [::product_id ::count]))
(s/def ::patch-order-products (s/coll-of ::order-products))


(defn patch-order-products
  [request]
  (let [order-id (u/str->int (:id (:params request)))
        order (o/order-by-id! order-id)
        valid? (and
                 order
                 (= (:status order) "new")
                 (s/valid? ::patch-order-products (:body request)))]
    (if valid?
      (try
        (o/update-order-products! order-id (:body request))
        {:body (o/order-by-id! order-id)}
        (catch Exception e
          (println e)
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Invalid input or order"}
       :status 400})))


(c/defroutes
  routes
  (c/GET "/:id{[0-9]+}/" [] order-details)
  (c/POST "/:id{[0-9]+}/accept/" [] accept-order)
  (c/POST "/:id{[0-9]+}/cancel/" [] cancel-order)
  (c/GET "/:id{[0-9]+}/products/" [] order-available-products)
  (c/PATCH "/:id{[0-9]+}/products/" [] patch-order-products)

  (c/GET "/active/" [] active-orders-list)
  (c/GET "/finished/" [] finished-orders))
