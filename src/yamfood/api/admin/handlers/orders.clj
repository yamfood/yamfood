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
    [yamfood.core.riders.core :as r]
    [yamfood.core.orders.core :as ord]
    [yamfood.core.kitchens.core :as k]
    [yamfood.core.params.core :as params]
    [yamfood.core.clients.core :as clients]
    [yamfood.core.products.core :as products]
    [yamfood.integrations.iiko.core :as iiko]
    [yamfood.telegram.helpers.status :as status]
    [yamfood.api.admin.handlers.products :as api.products])
  (:import (clojure.lang ExceptionInfo)))


(defonce open-orders (atom {}))


(defn reduce-active-orders
  [result order]
  (let [status (keyword (:status order))
        prev (get-in result [status :list])]
    (assoc-in result [status :list] (into prev [order]))))


(defn add-latency
  [order]
  (let [created-at (tc/from-sql-date (:created_at order))
        late-at (t/plus created-at (t/minutes 60))
        latency (if (t/after? (t/now) late-at)
                  (t/in-minutes (t/interval late-at (t/now)))
                  nil)]
    (assoc order :latency latency)))


(defn add-viewers!
  [order]
  (let [order-id (:id order)
        viewers ((keyword (str order-id)) @open-orders)]
    (assoc order :viewers (->> viewers (map :name) (distinct)))))


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
         (map add-viewers!)
         (map add-latency))))


(defn active-orders-list
  [_]
  {:body (get-active-orders!)})


(defn order-by-id!
  [order-id]
  (update
    (o/order-by-id! order-id)
    :products
    (fn [products]
      (map #(update % :name :ru) products))))


(defn order-details
  [request]
  (let [order-id (u/str->int (:id (:params request)))]
    {:body (order-by-id! order-id)}))


(defn accept-order
  [request]
  (let [admin-id (:id (:admin request))
        order-id (u/str->int (:id (:params request)))
        order (order-by-id! order-id)
        params (params/params!)
        send-to-iiko? (:iiko-enabled? params)
        acceptable? (some? (get #{(:new o/order-statuses)
                                  (:pending o/order-statuses)}
                                (:status order)))]

    (cond
      (= (count (:products order)) 0)
      {:body   {:error {:products "Can't accept order with no products"}}
       :status 400}

      acceptable?
      (try
        (when send-to-iiko? (iiko/create-order! order))
        (when (and (= (:pending o/order-statuses) (:status order))
                   ;; TODO move yamfood.telegram.handlers.utils/card-payment to global utils
                   (= (:payment order) "card"))
          (o/update! order-id {:is_payed true}))
        (o/accept-order! (:id order) admin-id)
        (status/notify-order-accepted! (:id order))
        {:body (get-active-orders!)}
        (catch ExceptionInfo e
          (println e)
          {:body   {:error "Iiko error"}
           :status 500})
        (catch Exception e
          (println e)
          {:body   {:error "Unexpected error"}
           :status 500}))

      :else
      {:body   {:error "Can't accept order in this status"}
       :status 400})))


(defn create-order
  [request]
  (let [body (select-keys (:body request)
                          [:id :client_id :notes :latitude :longitude :address])
        params (params/params!)
        valid? (s/valid? ::create-order body)
        kitchen (k/nearest-kitchen!
                  (:bot_id (clients/client-with-id! (int (:client_id body))))
                  (:longitude body)
                  (:latitude body))]
    (cond
      (nil? kitchen)
      {:body   {:error {:kitchen_id "Not available"}}
       :status 400}

      valid?
      (try
        {:body (let [order-id (o/create-pending-order!
                                (:client_id body)
                                (:longitude body)
                                (:latitude body)
                                (:address body)
                                (:id kitchen)
                                (or (:notes body) "")
                                "cash"
                                (or (:delivery-cost params) 10000))]
                 (order-by-id! order-id))}
        (catch Exception e
          (println e)
          {:body   {:error "Unexpected error"}
           :status 500}))

      :else
      {:body   {:error "Invalid input or order"}
       :status 400})))


(s/def ::reason string?)
(s/def ::cancel-order-body
  (s/keys :opt-un [::reason]))


(defn cancel-order
  [request]
  (let [order-id (u/str->int (:id (:params request)))
        order (order-by-id! order-id)
        cancelable? (u/in? o/cancelable-order-statuses
                           (:status order))
        valid? (and
                 cancelable?
                 (s/valid? ::cancel-order-body (:body request)))]
    (if valid?
      (try
        (o/cancel-order! (:id order) (:id (:admin request)))
        (status/notify-order-canceled! (:id order)
                                       (:reason (:body request)))
        {:body (get-active-orders!)}
        (catch Exception e
          (println e)
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
  [socket message]
  (let [data (message->clj message)
        order-id (:order data)
        token (:token data)
        admin-name (:name (a/admin-by-token! token))]
    (swap! open-orders
           update (keyword (str order-id))
           (partial cons {:socket socket
                          :name   admin-name}))))


(defn close-fn!
  [socket message]
  (fn []
    (let [data (message->clj message)
          order-id (:order data)]
      (swap! open-orders
             (fn [orders]
               (->> (update orders (keyword (str order-id))
                            (partial remove
                                     (comp #{socket} :socket)))
                    (medley.core/filter-vals seq)))))))


(defn ws-handler
  [req]
  (if-let [socket (try
                    @(http/websocket-connection req)
                    (catch Exception e
                      nil))]
    (do
      (let [message @(stream/take! socket)]
        (consumer! socket message)
        (stream/on-closed
          socket
          (close-fn! socket message)))
      nil)
    non-websocket-request))


(defn order-available-products
  [request]
  (let [order-id (u/str->int (:id (:params request)))
        order (order-by-id! order-id)]
    (if order
      (try
        {:body (->> (products/products-by-bot! (:bot_id order)
                                               (:kitchen_id order))
                    (map api.products/set-translations))}
        (catch Exception e
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Order not found"}
       :status 404})))


(defn fmt-order-log!
  [log]
  (let [payload (:payload log)
        rider-id (get payload "rider_id")
        admin-id (get payload "admin_id")
        payment-id (get payload "payment_id")]
    (assoc log :info (cond
                       rider-id (str "Курьер: " (:phone (r/rider-by-id! rider-id)))
                       admin-id (str "Администратор: " (:name (a/admin-by-id! admin-id)))
                       payment-id (str "Payme ID: " payment-id)))))


(defn order-logs
  [request]
  (let [order-id (u/str->int (:id (:params request)))
        logs (map fmt-order-log! (o/order-logs-by-order-id! order-id))]
    {:body logs}))


(s/def ::product_id int?)
(s/def ::client_id int?)
(s/def ::count int?)
(s/def ::comment string?)
(s/def ::order-products
  (s/keys :req-un [::product_id ::count]
          :opt-un [::comment]))

(s/def ::products (s/coll-of ::order-products))
(s/def ::notes string?)
(s/def ::address string?)
(s/def ::longitude float?)
(s/def ::latitude float?)
(s/def ::delivery_cost int?)

(s/def ::patch-order
  (s/keys :opt-un [::products ::notes ::address ::delivery_cost]))


(s/def ::create-order
  (s/keys :opt-un [::client_id ::notes ::address ::longitude ::latitude]))


(defn patch-order
  [request]
  (let [order-id (u/str->int (:id (:params request)))
        order (order-by-id! order-id)
        body (select-keys (:body request)
                          (concat [:products :notes :address :delivery_cost]
                                  (when (= "pending" (:status order))
                                    [:payment :is_payed])))
        valid? (and
                 order
                 (some? (#{"new" "pending"} (:status order)))
                 (s/valid? ::patch-order body))]
    (if valid?
      (try
        (o/update! order-id body)
        {:body (order-by-id! order-id)}
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

  (c/POST "/create/" [] create-order)
  (c/POST "/:id{[0-9]+}/accept/" [] accept-order)
  (c/POST "/:id{[0-9]+}/cancel/" [] cancel-order)
  (c/GET "/:id{[0-9]+}/products/" [] order-available-products)
  (c/GET "/:id{[0-9]+}/logs/" [] order-logs)

  (c/GET "/active/" [] active-orders-list)
  (c/GET "/finished/" [] finished-orders))
