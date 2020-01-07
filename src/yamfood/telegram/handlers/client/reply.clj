(ns yamfood.telegram.handlers.client.reply
  (:require
    [yamfood.core.users.core :as users]
    [yamfood.telegram.dispatcher :as d]))


(defn comment-handler
  [ctx]
  (let [update (:update ctx)
        message (:message update)
        text (:text message)
        user-id (:id (:user ctx))]
    {:run      {:function users/update-comment!
                :args     [user-id text]}
     :dispatch {:args [:order-confirmation-state]}}))


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
