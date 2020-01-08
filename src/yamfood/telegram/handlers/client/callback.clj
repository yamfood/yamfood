(ns yamfood.telegram.handlers.client.callback
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn callback-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        action (u/callback-action (:data query))]
    (case action
      "want" {:dispatch {:args [:c/detail-want]}}
      "detail+" {:dispatch {:args [:c/detail-inc]}}
      "detail-" {:dispatch {:args [:c/detail-dec]}}
      "basket" {:dispatch {:args [:c/basket]}}
      "basket+" {:dispatch {:args [:c/inc-basket-product]}}
      "basket-" {:dispatch {:args [:c/dec-basket-product]}}
      "to-order" {:dispatch {:args [:c/to-order]}}
      "request-location" {:dispatch {:args [:c/request-location]}}
      "change-payment-type" {:dispatch {:args [:c/change-payment-type]}}
      "change-comment" {:dispatch {:args [:c/change-comment]}}
      "create-order" {:dispatch {:args [:c/create-order]}}
      "invoice" {:dispatch {:args [:c/send-invoice]}}
      "cancel-invoice" {:dispatch {:args [:c/cancel-invoice]}}
      {:answer-callback {:callback_query_id (:id query)
                         :text              " "}})))


(d/register-event-handler!
  :c/callback
  callback-handler)
