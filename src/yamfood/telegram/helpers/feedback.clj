(ns yamfood.telegram.helpers.feedback
  (:require
    [morse.api :as t]
    [environ.core :refer [env]]
    [yamfood.core.orders.core :as o]))


(defn feedback-request-markup
  [order-id]
  {:inline_keyboard
   [[{:text "\uD83E\uDD2C" :callback_data (str "feedback/" order-id "/0")}
     {:text "☹️" :callback_data (str "feedback/" order-id "/1")}
     {:text "\uD83D\uDE10" :callback_data (str "feedback/" order-id "/2")}
     {:text "\uD83D\uDE0B" :callback_data (str "feedback/" order-id "/3")}
     {:text "\uD83E\uDD29" :callback_data (str "feedback/" order-id "/4")}]]})


(defn send-feedback-request!
  [order-id]
  (let [order (o/order-by-id! order-id)
        chat-id (:tid order)]
    (t/send-text
      (env :bot-token)
      chat-id
      {:reply_markup (feedback-request-markup order-id)}
      "Оцените пожалуйста заказ!")))


