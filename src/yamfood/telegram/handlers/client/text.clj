(ns yamfood.telegram.handlers.client.text
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.translation.core :refer [translate]]))


(def update-location-text "Обновить локацию")

(defn text-handler
  [ctx]
  (let [update (:update ctx)
        message (:message update)
        client (:client ctx)
        text (:text message)]
    (cond
      (= (:step (:payload client)) u/phone-step) {:dispatch {:args [:c/phone]}}
      (= (:step (:payload client)) u/feedback-step) {:dispatch {:args [:c/text-feedback]}}
      (= (:step (:payload client)) u/phone-confirmation-step) {:dispatch {:args [:c/confirm-phone]}}
      (= (:step (:payload client)) u/comment-step) {:dispatch {:args [:c/change-comment]}}
      (= text update-location-text) {:dispatch {:args [:c/request-location]}}
      :else {:dispatch {:args [:c/start]}})))


(d/register-event-handler!
  :c/no-product-text
  text-handler)
