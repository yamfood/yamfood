(ns yamfood.telegram.effects
  (:require [yamfood.telegram.dispatcher :as d]
            [morse.api :as t]
            [clj-http.client :as http]
            [environ.core :refer [env]]))


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


(def base-url "https://api.telegram.org/bot")
(defn edit-reply-markup
  "Edits only the reply markup of message
  (https://core.telegram.org/bots/api#editmessagereplymarkup)"
  ([token chat-id message-id reply-markup] (edit-reply-markup token chat-id message-id {} reply-markup))
  ([token chat-id message-id options reply-markup]
   (let [url   (str base-url token "/editMessageReplyMarkup")
         query (into {:chat_id chat-id :reply_markup reply-markup :message_id message-id} options)
         resp  (http/post url {:content-type :json
                               :as           :json
                               :form-params  query})]
     (-> resp :body))))

(d/register-effect-handler!
  :edit-reply-markup
  (fn [ctx effect]
    (println (str "\n\n###" effect))
    (edit-reply-markup
      (:token ctx)
      (:chat_id effect)
      (:message_id effect)
      (:reply_markup effect))))
