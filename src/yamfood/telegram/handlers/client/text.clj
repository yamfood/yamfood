(ns yamfood.telegram.handlers.client.text
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(def update-location-text "Обновить локацию")

(defn text-handler
  [ctx]
  (let [update (:update ctx)
        message (:message update)
        user (:user ctx)
        chat-id (:id (:from message))
        text (:text message)]
    (cond
      (= (:step (:payload user)) u/phone-step) {:dispatch {:args [:c/phone]}}
      (= (:step (:payload user)) u/phone-confirmation-step) {:dispatch {:args [:c/confirm-phone]}}
      (= text update-location-text) {:dispatch {:args [:c/request-location]}}
      :else {:send-text {:chat-id chat-id
                         :text    "Не понял"}})))


(d/register-event-handler!
  :c/no-product-text
  text-handler)
