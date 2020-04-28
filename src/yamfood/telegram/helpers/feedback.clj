(ns yamfood.telegram.helpers.feedback
  (:require
    [morse.api :as t]
    [environ.core :refer [env]]
    [yamfood.core.orders.core :as o]
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
        lang (or (:lang order) :ru)
        chat-id (:tid order)]
    (t/send-text
      (env :bot-token)
      chat-id
      {:reply_markup (feedback-request-markup order-id)}
      (translate lang :request-feedback-message))))
