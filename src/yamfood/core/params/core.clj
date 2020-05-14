(ns yamfood.core.params.core
  (:require
    [honeysql.core :as hs]
    [environ.core :refer [env]]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [clojure.set :refer [difference]]))


(def params-query
  {:select   [:params.id
              :params.name
              :params.key
              :params.value
              :params.docs]
   :from     [:params]
   :order-by [:params.id]})


(def all-params
  {:delivery-cost       {:name    "Сумма за доставку"
                         :docs    "Сумма за доставку по умолчанию"
                         :default "10000"
                         :adapter read-string}
   :iiko-enabled?       {:name    "Отправлять заказы в iiko?"
                         :docs    "true - да | false - нет"
                         :default "false"
                         :adapter read-string}
   :iiko-user-id        {:name    "IIKO User ID"
                         :docs    "Логин от системы IIKO"
                         :default (env :iiko-user-id)
                         :adapter nil}
   :iiko-user-secret    {:name    "IIKO User Secret"
                         :docs    "Пароль от системы IIKO"
                         :default (env :iiko-user-secret)
                         :adapter nil}
   :iiko-home           {:name    "IIKO Home"
                         :docs    "Дом, который будет подставляться в заказы для IIKO"
                         :default ""
                         :adapter nil}
   :iiko-delivery-id    {:name    "IIKO Delivery ID"
                         :docs    "ID позиции доставки из IIKO"
                         :default ""
                         :adapter nil}
   :iiko-street         {:name    "IIKO Street"
                         :docs    "Улица,которая будет подставляться в заказы для IIKO"
                         :default ""
                         :adapter nil}
   :playmobile-url      {:name    "PlayMobile URL"
                         :docs    "Адрес API"
                         :default "http://91.204.239.44/broker-api/send"
                         :adapter nil}
   :playmobile-login    {:name    "PlayMobile Логин"
                         :docs    "Логин от системы SMS информирования"
                         :default ""
                         :adapter nil}
   :playmobile-password {:name    "PlayMobile Пароль"
                         :docs    "Пароль от системы SMS информирования"
                         :default ""
                         :adapter nil}
   :notifier-bot-token  {:name    "Токен бота для оповещений"
                         :docs    "Токен бота для оповещений"
                         :default ""
                         :adapter nil}
   :feedback-chat-id    {:name    "ID чата отзывов"
                         :docs    "ID чата в который бот будет слать отзывы клиентов"
                         :default -1
                         :adapter read-string}})


(defn params-detail-list!
  []
  (->> (-> params-query
           (hs/format))
       (jdbc/query db/db)))


(defn update!
  [id param]
  (jdbc/update!
    db/db
    "params"
    param
    ["id = ?" id]))


(defn params-reducer
  [r param]
  (let [key (keyword (:key param))
        adapter (:adapter (get all-params key))
        value (if adapter
                (adapter (:value param))
                (:value param))]
    (assoc r key value)))


(defn params->map
  [params-list]
  (reduce
    params-reducer
    {}
    params-list))


(defn params!
  []
  (->> params-query
       (hs/format)
       (jdbc/query db/db)
       (params->map)))


(defn absent-params!
  []
  (let [existing (set (keys (params!)))
        all (set (keys all-params))
        to-create (vec (difference all existing))]
    (select-keys all-params to-create)))


(defn create-params!
  [params]
  (let [params (map (fn [param]
                      (let [key (first param)
                            p (second param)]
                        {:key   (name key)
                         :name  (:name p)
                         :value (:default p)
                         :docs  (:docs p)}))
                    (seq params))]
    (jdbc/insert-multi!
      db/db
      "params"
      params)))


(defn sync-params!
  []
  (-> (absent-params!)
      (create-params!)))
