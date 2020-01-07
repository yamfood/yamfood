(ns yamfood.telegram.handlers.rider.text
  (:require [yamfood.telegram.dispatcher :as d]))


(defn rider-text-handler
  [ctx]
  (let [update (:update ctx)
        message (:message update)
        chat-id (:id (:from message))]
    {:send-text {:chat-id chat-id
                 :text "Hello, Rider!"}}))


(d/register-event-handler!
  :r/text
  rider-text-handler)



