(ns yamfood.telegram.core
  (:require [yamfood.core.users.core :as users]
            [yamfood.telegram.dispatcher :as d]
            [yamfood.telegram.events]
            [yamfood.telegram.effects]
            [morse.api :as t]
            [environ.core :refer [env]]))


(def token (env :bot-token))


(defn log
  [message]
  (println (str "\n\n\n ### \n" message "\n\n\n")))


(defn get-tid-from-update ; Make it work with all updates!
  [update]
  (let [message (:message update)]
    (:id (:from message))))


(defn build-ctx
  [update]
  {:token token
   :user  (users/get-user-by-tid! (get-tid-from-update update))})


(defn process-message
  [ctx update]
  (let [message (:message update)
        text (:text message)
        contact (:contact message)]
    (cond
      (= text "/start") (d/dispatch ctx [:start update])
      contact (d/dispatch ctx [:contact update])
      text (d/dispatch ctx [:text message]))))


(defn process-updates
  [request]
  (log (:body request))
  (let [update (:body request)
        message (:message update)
        inline-query (:inline_query update)
        ctx (build-ctx update)]
    (if message
      (process-message ctx update))
    (if inline-query
      (d/dispatch ctx [:inline update])))
  {:body "OK"})

;(t/set-webhook token "https://f7a8fb59.ngrok.io/updates")


