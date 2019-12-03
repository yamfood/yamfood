(ns yamfood.telegram.effects
  (:require [yamfood.telegram.dispatcher :as d]
            [morse.api :as t]
            [yamfood.telegram.methods :as -t]
            [environ.core :refer [env]]))


(d/register-effect-handler!
  :dispatch
  (fn [ctx effect]
    (d/dispatch! ctx (:args effect))))


(d/register-effect-handler!
  :core
  (fn [_ effect]
    (println effect)
    (let [core-func (:function effect)
          on-complete (:on-complete effect)]
      (if on-complete
        (on-complete (core-func))
        (core-func)))))


(d/register-effect-handler!
  :answer-inline
  (fn [ctx effect]
    (t/answer-inline
      (:token ctx)
      (:inline-query-id effect)
      (:options effect)
      (:results effect))))


(d/register-effect-handler!
  :answer-callback
  (fn [ctx effect]
    (t/answer-callback
      (:token ctx)
      (:callback_query_id effect)
      (:text effect)
      (:show_alert effect))))


(d/register-effect-handler!
  :send-text
  (fn [ctx effect]
    (t/send-text
      (:token ctx)
      (:chat-id effect)
      (:options effect)
      (:text effect))))


(d/register-effect-handler!
  :send-photo
  (fn [ctx effect]
    (t/send-photo
      (:token ctx)
      (:chat-id effect)
      (:options effect)
      (:photo effect))))


(d/register-effect-handler!
  :delete-message
  (fn [ctx effect]
    (t/delete-text
      (:token ctx)
      (:chat-id effect)
      (:message-id effect))))


(d/register-effect-handler!
  :edit-reply-markup
  (fn [ctx effect]
    (-t/edit-reply-markup
      (:token ctx)
      (:chat_id effect)
      (:message_id effect)
      (:reply_markup effect))))
