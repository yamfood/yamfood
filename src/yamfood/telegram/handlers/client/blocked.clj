(ns yamfood.telegram.handlers.client.blocked
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn blocked-handler
  [ctx]
  (let [chat-id (u/chat-id (:update ctx))]
    {:send-text {:chat-id chat-id
                 :text "Вы заблокированы, обратитесь в поддержку"}}))


(d/register-event-handler!
  :c/blocked
  blocked-handler)
