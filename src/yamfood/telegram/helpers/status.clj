(ns yamfood.telegram.helpers.status
  (:require
    [morse.api :as t]
    [environ.core :refer [env]]
    [yamfood.core.params.core :as p]
    [yamfood.core.orders.core :as o]
    [yamfood.core.bots.core :as bots]
    [yamfood.telegram.translation.core :refer [translate]]))


(defn notify-order-accepted!
  [order-id]
  (let [params (p/params!)
        order (o/order-by-id! order-id)
        bot (bots/bot-by-id! (:bot_id order))
        lang (or (:lang order) :ru)]
    (when (:tid order)
      (t/send-text
        (:token bot)
        (:tid order)
        {:parse_mode "markdown"}
        (translate lang :status-on-kitchen (:delivery-time params))))))


(defn notify-order-canceled!
  ([order-id]
   (notify-order-canceled! order-id nil))
  ([order-id reason]
   (let [order (o/order-by-id! order-id)
         bot (bots/bot-by-id! (:bot_id order))
         lang (or (:lang order) :ru)
         reason (or reason
                    (translate lang :status-canceled))]
     (when (:tid order)
       (t/send-text
         (:token bot)
         (:tid order)
         {:parse_mode "markdown"}
         reason)))))


(defn notify-order-on-way!
  [order-id]
  (let [order (o/order-by-id! order-id)
        bot (bots/bot-by-id! (:bot_id order))
        lang (or (:lang order) :ru)]
    (when (:tid order)
      (t/send-text
        (:token bot)
        (:tid order)
        {:parse_mode "markdown"}
        (translate lang :status-on-way)))))
