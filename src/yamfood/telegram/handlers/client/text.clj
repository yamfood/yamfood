(ns yamfood.telegram.handlers.client.text
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.translation.core :refer [translate]]))


(def update-location-text "Обновить локацию")

(defn text-handler
  [ctx]
  (let [update (:update ctx)
        lang (:lang ctx)
        message (:message update)
        client (:client ctx)
        chat-id (:id (:from message))
        text (:text message)]
    (cond
      (= (:step (:payload client)) u/phone-step) {:dispatch {:args [:c/phone]}}
      (= (:step (:payload client)) u/phone-confirmation-step) {:dispatch {:args [:c/confirm-phone]}}
      (= text update-location-text) {:dispatch {:args [:c/request-location]}}
      :else {:send-text {:chat-id chat-id
                         :text    (translate lang :unhandled-text)}})))


(d/register-event-handler!
  :c/no-product-text
  text-handler)
