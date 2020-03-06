(ns yamfood.telegram.handlers.client.reply
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.clients.core :as clients]))


(defn comment-handler
  [ctx]
  (let [update (:update ctx)
        message (:message update)
        text (:text message)
        client (:client ctx)]
    {:run      {:function clients/update-payload!
                :args     [(:id client) (assoc
                                          (:payload client)
                                          :comment
                                          text)]}
     :dispatch {:args [:c/order-confirmation-state]}}))


(def write-comment-text "Напишите свой комментарий к заказу")
(defn message-with-reply-handler
  [ctx]
  (let [update (:update ctx)
        message (:message update)
        reply_message (:reply_to_message message)
        reply_text (:text reply_message)]
    (cond
      (= reply_text write-comment-text) (comment-handler ctx)
      :else {})))


(d/register-event-handler!
  :c/reply
  message-with-reply-handler)
