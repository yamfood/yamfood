(ns yamfood.telegram.core
  (:require
    [yamfood.telegram.events]
    [yamfood.telegram.effects]
    [environ.core :refer [env]]
    [yamfood.core.users.core :as users]
    [yamfood.telegram.dispatcher :as d]))


(defn tid-from-update                                   ; TODO: Make it work with all updates!
  [update]
  (let [message (:message update)
        callback (:callback_query update)
        inline (:inline_query update)]
    (cond
      message (:id (:from message))
      callback (:id (:from callback))
      inline (:id (:from inline)))))


(defn build-ctx!
  [update]
  {:token          (env :bot-token)
   :payments-token (env :payments-token)
   :user           (users/user-with-tid!
                     (tid-from-update update))})


(defn process-message
  [ctx update]
  (let [message (:message update)
        text (:text message)
        contact (:contact message)
        location (:location message)
        reply-to (:reply_to_message message)]
    (cond
      (= text "/start") (d/dispatch! ctx [:start update])
      location (d/dispatch! ctx [:location update])
      contact (d/dispatch! ctx [:contact update])
      reply-to (d/dispatch! ctx [:reply update])
      text (d/dispatch! ctx [:text message]))))


(defn update-handler!
  [update]
  (let [message (:message update)
        inline-query (:inline_query update)
        callback-query (:callback_query update)
        ctx (build-ctx! update)]
    (if message
      (process-message ctx update))
    (if inline-query
      (d/dispatch! ctx [:inline update]))
    (if callback-query
      (d/dispatch! ctx [:callback update]))))


(defn telegram-handler!
  [request]
  (try
    (update-handler! (:body request))
    (catch Exception e
      (println
        (format
          "\n\n %s \n\n"
          {:update (:body request) :error e}))))
  {:body "OK"})

;(morse.api/set-webhook (env :bot-token) "https://36b851f9.ngrok.io/updates")