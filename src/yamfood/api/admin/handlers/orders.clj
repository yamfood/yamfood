(ns yamfood.api.admin.handlers.orders
  (:require
    [aleph.http :as http]
    [yamfood.utils :as u]
    [clj-time.core :as t]
    [compojure.core :as c]
    [clj-time.coerce :as tc]
    [clojure.spec.alpha :as s]
    [manifold.stream :as stream]
    [clojure.data.json :as json]
    [yamfood.api.pagination :as p]
    [yamfood.core.admin.core :as a]
    [yamfood.core.orders.core :as o]
    [yamfood.core.orders.core :as ord]
    [yamfood.core.products.core :as products]
    [yamfood.telegram.helpers.status :as status]))


(defonce open-orders (atom {}))


(defn reduce-active-orders
  [result order]
  (let [status (keyword (:status order))
        prev (get-in result [status :list])]
    (assoc-in result [status :list] (into prev [order]))))


(defn add-latency
  [order]
  (let [created-at (tc/from-sql-date (:created_at order))
        late-at (t/plus created-at (t/minutes 30))
        latency (if (t/after? (t/now) late-at)
                  (t/in-minutes (t/interval late-at (t/now)))
                  nil)]
    (assoc order :latency latency)))


(defn add-viewer!
  [order]
  (let [order-id (:id order)
        viewer ((keyword (str order-id)) @open-orders)]
    (assoc order :viewer viewer)))


(defn empty-structure
  []
  (apply
    merge
    (map #(hash-map (keyword %) {:late 0
                                 :list []})
         ord/active-order-statuses)))


(defn get-active-orders!
  []
  (reduce
    reduce-active-orders
    (empty-structure)
    (->> (ord/active-orders!)
         (map add-viewer!)
         (map add-latency))))


(defn active-orders-list
  [_]
  {:body (get-active-orders!)})


(defn order-details
  [request]
  (let [order-id (u/str->int (:id (:params request)))]
    {:body (update
             (o/order-by-id! order-id)
             :products
             (fn [products]
               (map #(update % :name :ru) products)))}))


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


(s/def ::reason string?)
(s/def ::cancel-order-body
  (s/keys :opt-un [::reason]))


(defn cancel-order
  [request]
  (let [order-id (u/str->int (:id (:params request)))
        order (o/order-by-id! order-id)
        cancelable? (u/in? o/cancelable-order-statuses
                           (:status order))
        valid? (and
                 cancelable?
                 (s/valid? ::cancel-order-body (:body request)))]
    (if valid?
      (do
        (if (do (o/cancel-order! (:id order))
                (status/notify-order-canceled! (:id order)
                                               (:reason (:body request))))
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
    (swap! open-orders assoc (keyword (str order-id)) (:name admin))))


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
(s/def ::comment string?)
(s/def ::order-products
  (s/keys :req-un [::product_id ::count]
          :opt-un [::comment]))

(s/def ::products (s/coll-of ::order-products))
(s/def ::notes string?)
(s/def ::address string?)
(s/def ::delivery_cost int?)

(s/def ::patch-order
  (s/keys :opt-un [::products ::notes ::address ::delivery_cost]))


(defn patch-order
  [request]
  (let [order-id (u/str->int (:id (:params request)))
        order (o/order-by-id! order-id)
        body (select-keys (:body request)
                          [:products :notes :address :delivery-cost])
        valid? (and
                 order
                 (= (:status order) "new")
                 (s/valid? ::patch-order body))]
    (if valid?
      (try
        (o/update! order-id body)
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
  (c/PATCH "/:id{[0-9]+}/" [] patch-order)

  (c/POST "/:id{[0-9]+}/accept/" [] accept-order)
  (c/POST "/:id{[0-9]+}/cancel/" [] cancel-order)
  (c/GET "/:id{[0-9]+}/products/" [] order-available-products)

  (c/GET "/active/" [] active-orders-list)
  (c/GET "/finished/" [] finished-orders))
