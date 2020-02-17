(ns yamfood.telegram.handlers.client.text
  (:require
    [yamfood.telegram.dispatcher :as d]))


(def update-location-text "Обновить локацию")

(defn text-handler
  [ctx]
  (let [update (:update ctx)
        message (:message update)
        chat-id (:id (:from message))
        text (:text message)]
    (cond
      (= text update-location-text) {:dispatch {:args [:c/request-location]}}
      :else {:send-text {:chat-id chat-id
                         :text    "Не понял"}})))


(d/register-event-handler!
  :c/no-product-text
  text-handler)
