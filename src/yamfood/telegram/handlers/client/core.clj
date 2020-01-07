(ns yamfood.telegram.handlers.client.core
  (:require
    [environ.core :refer [env]]
    [yamfood.core.users.core :as users]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn build-ctx!
  [update]
  {:token          (env :bot-token)
   :payments-token (env :payments-token)
   :update         update
   :user           (users/user-with-tid!
                     (u/tid-from-update update))})


(defn process-message
  [ctx update]
  (let [message (:message update)
        text (:text message)
        contact (:contact message)
        location (:location message)
        reply-to (:reply_to_message message)]
    (cond
      (= text "/start") (d/dispatch! ctx [:c/start])
      location (d/dispatch! ctx [:c/location])
      contact (d/dispatch! ctx [:c/contact])
      reply-to (d/dispatch! ctx [:c/reply])
      text (d/dispatch! ctx [:c/text]))))


(defn client-update-handler!
  [update]
  (let [message (:message update)
        inline-query (:inline_query update)
        callback-query (:callback_query update)
        ctx (build-ctx! update)]
    (if message
      (process-message ctx update))
    (if inline-query
      (d/dispatch! ctx [:c/inline]))
    (if callback-query
      (d/dispatch! ctx [:c/callback]))))
