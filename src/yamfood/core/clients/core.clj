(ns yamfood.core.clients.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]))


(defn clients-active-order-query
  [client-id]
  {:select   [:orders.id]
   :from     [:orders :order_logs]
   :where    [:and
              [:= :orders.client_id client-id]
              [:= :orders.id :order_logs.order_id]
              [:= :order_logs.status "new"]]
   :order-by [[:orders.created_at :desc]]
   :limit    1})


(def client-query
  {:select [:clients.id
            :clients.phone
            :clients.tid
            :clients.payload
            :clients.is_blocked
            [(clients-active-order-query :clients.id) :active_order_id]
            [:baskets.id :basket_id]]
   :from   [:clients :baskets]
   :where  [:= :baskets.client_id :clients.id]})


(defn keywordize
  [data]
  (into
    {}
    (for [[k v] data]
      [(keyword k) (if (map? v) (keywordize v) v)])))


(defn fmt-payload
  [client]
  (let [payload (:payload client)]
    (assoc client :payload (keywordize payload))))


(defn client-with-tid-query
  [tid]
  (hs/format (hh/merge-where client-query [:= :clients.tid tid])))



(defn client-with-id-query
  [id]
  (hs/format (hh/merge-where client-query [:= :clients.id id])))


(defn client-with-basket-id-query
  [basket-id]
  (hs/format (hh/merge-where client-query [:= :baskets.id basket-id])))


(defn client-with-basket-id!
  [basket-id]
  (->> (client-with-basket-id-query basket-id)
       (jdbc/query db/db)
       (map fmt-payload)
       (first)))


(defn client-with-tid!
  [tid]                                                     ; TODO: CACHE!
  (->> (client-with-tid-query tid)
       (jdbc/query db/db)
       (map fmt-payload)
       (first)))


(defn client-with-id!
  [id]
  (->> (client-with-id-query id)
       (jdbc/query db/db)
       (map fmt-payload)
       (first)))


(defn clients-list-query
  [offset limit]
  {:select   [:clients.id
              :clients.tid
              :clients.name
              :clients.phone
              :clients.payload
              :clients.is_blocked]
   :from     [:clients]
   :order-by [:clients.id]
   :offset   offset
   :limit    limit})


(def clients-count-query
  {:select [[:%count.clients.id :count]]
   :from   [:clients]})


(defn clients-count!
  ([]
   (clients-count! nil))
  ([where]
   (->> (-> clients-count-query
            (hh/merge-where where))
        (hs/format)
        (jdbc/query db/db)
        (first)
        (:count))))


(defn clients-list!
  ([]
   (clients-list! 0 100))
  ([offset limit]
   (clients-list! offset limit nil))
  ([offset limit where]
   (->> (-> (clients-list-query offset limit)
            (hh/merge-where where))
        (hs/format)
        (jdbc/query db/db)
        (map fmt-payload))))


(defn insert-client!
  [tid name payload]
  (first (jdbc/insert! db/db "clients" {:tid tid :name name :payload payload})))


(defn init-basket!
  [client-id]
  (jdbc/insert! db/db "baskets" {:client_id client-id}))


(defn create-client!
  ([tid name]
   (create-client! tid name {}))
  ([tid name payload]
   (let [client (insert-client! tid name payload)]
     (init-basket! (:id client)))))


(defn update-location-query
  [client-id lon lat]
  (hs/format {:update :clients
              :set    {:location (hs/raw (format "POINT(%s, %s)" lon lat))}
              :where  [:= :id client-id]}))


(defn update!
  [client-id row]
  (jdbc/update! db/db "clients" row ["id = ?" client-id]))


(defn update-by-tid!
  [tid row]
  (jdbc/update! db/db "clients" row ["tid = ?" tid]))


(defn update-location!
  "Lon - longitude (X)
   Lat - latitude (Y)"
  [client-id lon lat]
  (->> (update-location-query client-id lon lat)
       (jdbc/execute! db/db)))


(defn update-phone!
  [client-id phone]
  (jdbc/update! db/db "clients" {:phone phone} ["id = ?" client-id]))


(defn update-payload!
  [client-id payload]
  (jdbc/update! db/db "clients" {:payload payload} ["id = ?" client-id]))
