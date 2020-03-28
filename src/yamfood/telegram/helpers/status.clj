(ns yamfood.telegram.helpers.status
  (:require
    [morse.api :as t]
    [environ.core :refer [env]]
    [yamfood.core.orders.core :as o]))


(def token (env :bot-token))


(defn notify-order-accepted!
  [order-id]
  (let [order (o/order-by-id! order-id)]
    (t/send-text
      token
      (:tid order)
      {:parse_mode "markdown"}
      "Ваш заказ уже начал готовиться!")))


(defn notify-order-canceled!
  [order-id]
  (let [order (o/order-by-id! order-id)]
    (t/send-text
      token
      (:tid order)
      {:parse_mode "markdown"}
      "Заказ отменен (")))


(defn notify-order-on-way!
  [order-id]
  (let [order (o/order-by-id! order-id)]
    (t/send-text
      token
      (:tid order)
      {:parse_mode "markdown"}
      "Райдер уже в пути!")))
