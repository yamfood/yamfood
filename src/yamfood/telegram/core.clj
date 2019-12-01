(ns yamfood.telegram.core
  (:require [yamfood.core.users.core :as users]
            [yamfood.telegram.dispatcher :as d]
            [yamfood.telegram.events]
            [yamfood.telegram.effects]
            [morse.api :as t]
            [environ.core :refer [env]]))


(def token (env :bot-token))


(defn get-tid-from-update                                   ; TODO: Make it work with all updates!
  [update]
  (let [message (:message update)
        callback (:callback_query update)
        inline (:inline_query update)]
    (cond
      message (:id (:from message))
      callback (:id (:from callback))
      inline (:id (:from inline)))))


(defn build-ctx
  [update]
  {:token token
   :user  (users/user-with-tid! (get-tid-from-update update))})


(defn process-message
  [ctx update]
  (let [message (:message update)
        text (:text message)
        contact (:contact message)
        location (:location message)]
    (cond
      (= text "/start") (d/dispatch ctx [:start update])
      location (d/dispatch ctx [:location update])
      contact (d/dispatch ctx [:contact update])
      text (d/dispatch ctx [:text message]))))


(defn handle-update
  [update]
  (let [message (:message update)
        inline-query (:inline_query update)
        callback-query (:callback_query update)
        ctx (build-ctx update)]
    (if message
      (process-message ctx update))
    (if inline-query
      (d/dispatch ctx [:inline update]))
    (if callback-query
      (d/dispatch ctx [:callback update]))))


(defn telegram-handler
  [request]
  (try
    (handle-update (:body request))
    (catch Exception e
      (println
        (format
          "\n\n %s \n\n"
          {:update (:body request) :error e}))))
  {:body "OK"})

;(t/set-webhook token "https://19ab8c3c.ngrok.io/updates")