(ns yamfood.telegram.handlers.reply
  (:require [yamfood.telegram.dispatcher :as d]))


(def write-comment-text "Напишите свой комментарий к заказу")
(defn handle-message-with-reply
  [ctx update]
  (let [message (:message update)
        reply_message (:reply_to_message message)
        reply_text (:text reply_message)]
    (cond
      (= reply_text write-comment-text)
      {:core {:function #(d/dispatch! ctx [:make-order-state update])}})))


(d/register-event-handler!
  :reply
  handle-message-with-reply)
