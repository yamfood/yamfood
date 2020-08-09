(ns yamfood.api.admin.handlers.clients
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [yamfood.api.pagination :as p]
    [yamfood.core.specs.core :as cs]
    [yamfood.core.orders.core :as o]
    [yamfood.core.clients.core :as clients]
    [clojure.string :as str]
    [honeysql.helpers :as hh]
    [yamfood.core.bots.core :as bots]
    [clojure.tools.logging :as log]))


(defn distinct-comp
  ""
  {:added  "1.0"
   :static true}
  ([f coll]
   (let [step
         (fn step [xs seen]
           (lazy-seq
             ((fn [[x :as xs] seen]
                (when-let [s (seq xs)]
                  (if (some (partial f x) seen)
                    (recur (rest s) seen)
                    (cons x (step (rest s) (conj seen x))))))
              xs seen)))]
     (step coll #{}))))


(defn client-detail
  [request]
  (let [client-id (u/str->int (:id (:params request)))]
    {:body (-> (clients/client-with-id! client-id)
               (assoc :data [{:label "Количество завершенных заказов"
                              :value (o/client-finished-orders! client-id)}
                             {:label "Количество отмененных заказов"
                              :value (o/client-canceled-orders! client-id)}]
                      :last_orders (->> (o/client-last-orders! client-id)
                                        (remove (comp empty? :address))
                                        (distinct-comp
                                          #(or
                                             (str/includes? (:address %1) (:address %2))
                                             (str/includes? (:address %2) (:address %1))))
                                        (take 10))))}))


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


(defn create-client
  [request]
  (let [{:keys [bot_id phone name]} (-> (:body request)
                                        (update :phone u/str->int))
        bot (bots/bot-by-id! bot_id)
        client (clients/client-with-bot-id-and-phone! bot_id phone)
        errors (cond-> {}
                       (nil? bot)
                       (update :bot_id (partial concat ["Не существует"]))

                       (not (:is_active bot))
                       (update :bot_id (partial concat ["Бот не активирован"]))

                       (some? client)
                       (update :phone (partial concat ["Уже зарегистрирован для данного бота"]))

                       (empty? name)
                       (update :phone (partial concat ["Не может быть пустым"]))

                       :finally (not-empty))]
    (if (some? errors)
      {:body   {:error                 errors
                :conflicting_client_id (:id client)}
       :status 400}
      (try
        {:status 200
         :body   (select-keys
                   (clients/insert-client! nil bot_id name {} phone)
                   [:id :name :phone])}
        (catch Exception e
          (log/error "Failed to create user from dashboard" e)
          {:body   {:error "Unexpected error"}
           :status 500})))))


(c/defroutes
  routes
  (c/GET "/" [] clients-list)
  (c/POST "/create/" [] create-client)
  (c/GET "/:id{[0-9]+}/" [] client-detail)
  (c/PATCH "/:id{[0-9]+}/" [] client-patch))
