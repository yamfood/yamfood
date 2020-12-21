(ns yamfood.telegram.effects
  (:require
    [morse.api :as t]
    [environ.core :refer [env]]
    [yamfood.telegram.methods :as -t]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.utils :as utils]))


(d/register-effect-handler!
  :dispatch
  (fn [ctx effect]
    (let [rebuild-ctx (:rebuild-ctx effect)
          dispatch-args (:args effect)]
      (if rebuild-ctx
        (let [rebuild-fn! (:function rebuild-ctx)
              update (:update rebuild-ctx)
              token (:token rebuild-ctx)
              new-ctx (if token (rebuild-fn! token update)
                                (rebuild-fn! update))]
          (d/dispatch! new-ctx dispatch-args))
        (d/dispatch! ctx dispatch-args)))))


(d/register-effect-handler!
  :run
  (fn [ctx effect]
    (let [func (:function effect)
          args (:args effect)
          next-event (:next-event effect)
          result (apply func args)]
      (if next-event
        (d/dispatch! ctx [next-event result])
        result))))


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
    (let [result (:result (t/send-text
                            (:token ctx)
                            (:chat-id effect)
                            (:options effect)
                            (:text effect)))
          next-event (:next-event effect)]
      (if next-event
        (d/dispatch! ctx [next-event result])
        result))))



(d/register-effect-handler!
  :send-photo
  (fn [ctx effect]
    (t/send-photo
      (:token ctx)
      (:chat-id effect)
      (:options effect)
      (:photo effect))))


(d/register-effect-handler!
  :send-location
  (fn [ctx effect]
    (-t/send-location
      (:token ctx)
      (:chat-id effect)
      (:longitude effect)
      (:latitude effect)
      (:options effect))))


(d/register-effect-handler!
  :send-animation
  (fn [ctx effect]
    (-t/send-animation
      (:token ctx)
      (:chat-id effect)
      (:animation effect)
      (:options effect))))


(d/register-effect-handler!
  :delete-message
  (fn [ctx effect]
    (try
      (t/delete-text
        (:token ctx)
        (:chat-id effect)
        (:message-id effect))
      (catch Exception e
        (utils/log-error (:request ctx) e)))))


(d/register-effect-handler!
  :edit-reply-markup
  (fn [ctx effect]
    (-t/edit-reply-markup
      (:token ctx)
      (:chat_id effect)
      (:message_id effect)
      (:reply_markup effect))))


(d/register-effect-handler!
  :edit-message
  (fn [ctx effect]
    (t/edit-text
      (:token ctx)
      (:chat-id effect)
      (:message-id effect)
      (:options effect)
      (:text effect))))


(d/register-effect-handler!
  :edit-photo
  (fn [ctx effect]
    (-t/edit-caption
      (:token ctx)
      (:chat-id effect)
      (:message-id effect)
      (:options effect)
      (:caption effect))))


(d/register-effect-handler!
  :send-invoice
  (fn [ctx effect]
    (-t/send-invoice
      (:token ctx)
      (:payments-token ctx)
      (:chat-id effect)
      (:title effect)
      (:description effect)
      (:payload effect)
      (:currency effect)
      (:prices effect)
      (:options effect))))


(d/register-effect-handler!
  :answer-pre-checkout-query
  (fn [ctx effect]
    (-t/answer-pre-checkout-query
      (:token ctx)
      (:pre_checkout_query_id effect)
      (:ok effect)
      (:options effect))))
