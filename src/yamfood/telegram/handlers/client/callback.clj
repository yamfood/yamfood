(ns yamfood.telegram.handlers.client.callback
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn callback-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        action (u/callback-action (:data query))]
    (if (:phone (:client ctx))
      (case action
        "menu" {:dispatch {:args [:c/menu]}}
        "settings" {:dispatch {:args [:c/settings]}}
        "language" {:dispatch {:args [:c/change-language]}}
        "want" {:dispatch {:args [:c/detail-want]}}
        "construct" {:dispatch {:args [:c/construct]}}
        "construct-finish" {:dispatch {:args [:c/construct-finish]}}
        "detail+" {:dispatch {:args [:c/detail-inc]}}
        "detail-" {:dispatch {:args [:c/detail-dec]}}
        "basket" {:dispatch {:args [:c/basket]}}
        "basket-product-info" {:dispatch {:args [:c/basket-product-info]}}
        "basket+" {:dispatch {:args [:c/inc-basket-product]}}
        "basket-" {:dispatch {:args [:c/dec-basket-product]}}
        "confirm-location" {:dispatch {:args [:c/confirm-location]}}
        "to-order" {:dispatch {:args [:c/to-order]}}
        "to-order-confirmation" {:dispatch {:args [:c/order-confirmation-state]}}
        "request-location" {:dispatch {:args [:c/request-location]}}
        "request-phone" {:dispatch {:args [:c/request-phone]}}
        "switch-payment-type" {:dispatch {:args [:c/switch-payment-type]}}
        "send-last-comments" {:dispatch {:args [:c/send-last-comments]}}
        "set-comment-from-order" {:dispatch {:args [:c/set-comment-from-order]}}
        "create-order" {:dispatch {:args [:c/create-order]}}
        "cancel-invoice" {:dispatch {:args [:c/cancel-invoice]}}
        "feedback" {:dispatch {:args [:c/feedback]}}
        {:answer-callback {:callback_query_id (:id query)
                           :text              " "}})
      {:dispatch {:args [:c/start]}})))


(d/register-event-handler!
  :c/callback
  callback-handler)
