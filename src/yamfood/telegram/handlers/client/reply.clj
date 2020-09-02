(ns yamfood.telegram.handlers.client.reply
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.clients.core :as clients]))


(defn message-with-reply-handler
  [ctx]
  (let [update (:update ctx)
        message (:message update)
        reply_message (:reply_to_message message)
        reply_text (:text reply_message)]
    {}))


(d/register-event-handler!
  :c/reply
  message-with-reply-handler)
