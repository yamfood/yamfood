(ns yamfood.telegram.helpers.feedback
  (:require
    [morse.api :as t]
    [yamfood.core.orders.core :as o]
    [yamfood.core.bots.core :as bots]
    [yamfood.telegram.translation.core :refer [translate]]))


(defn feedback-request-markup
  [order-id]
  {:inline_keyboard
   [[{:text "\uD83E\uDD2C" :callback_data (str "feedback/" order-id "/\uD83E\uDD2C")}
     {:text "☹️" :callback_data (str "feedback/" order-id "/☹️")}
     {:text "\uD83D\uDE10" :callback_data (str "feedback/" order-id "/\uD83D\uDE10")}
     {:text "\uD83D\uDE0B" :callback_data (str "feedback/" order-id "/\uD83D\uDE0B")}
     {:text "\uD83E\uDD29" :callback_data (str "feedback/" order-id "/\uD83E\uDD29")}]]})


(defn send-feedback-request!
  [order-id]
  (let [order (o/order-by-id! order-id)
        bot (bots/bot-by-id! (:bot_id order))
        lang (or (:lang order) :ru)
        chat-id (:tid order)]
    (when (:tid order)
      (t/send-text
        (:token bot)
        chat-id
        {:reply_markup (feedback-request-markup order-id)}
        (translate lang :request-feedback-message)))))
