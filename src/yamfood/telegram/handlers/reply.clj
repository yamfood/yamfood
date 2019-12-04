(ns yamfood.telegram.handlers.reply
  (:require [yamfood.telegram.dispatcher :as d]
            [yamfood.core.users.core :as users]))


(defn handle-comment
  [ctx update]
  (let [message (:message update)
        text (:text message)
        user-id (:id (:user ctx))]
    {:core     {:function #(users/update-comment! user-id text)}
     :dispatch {:args [:pre-order-state update]}}))


(def write-comment-text "Напишите свой комментарий к заказу")
(defn handle-message-with-reply
  [ctx update]
  (println update)
  (let [message (:message update)
        reply_message (:reply_to_message message)
        reply_text (:text reply_message)]
    (cond
      (= reply_text write-comment-text) (handle-comment ctx update)
      :else (println "Can't find handler"))))


(d/register-event-handler!
  :reply
  handle-message-with-reply)
