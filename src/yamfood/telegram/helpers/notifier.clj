(ns yamfood.telegram.helpers.notifier
  (:require
    [morse.api :as t]
    [yamfood.core.params.core :as p]))


(defn feedback-text
  [order]
  (format (str "*Заказ #%s*\n"
               "*Клиент:* %s\n"
               "*Телефон:* +%s\n"
               "*Отзыв:* %s")
          (:id order)
          (:name order)
          (:phone order)
          (:rate order)))


(defn forward-feedback!
  [order]
  (let [params (p/params!)
        token (:notifier-bot-token params)
        chat-id (:feedback-chat-id params)]
    (t/send-text
      token
      chat-id
      {:parse_mode "markdown"}
      (feedback-text order))))
