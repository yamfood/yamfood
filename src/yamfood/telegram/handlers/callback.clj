(ns yamfood.telegram.handlers.callback
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn callback-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        action (u/callback-action (:data query))]
    (case action
      "want" {:dispatch {:args [:detail-want]}}
      "detail+" {:dispatch {:args [:detail-inc]}}
      "detail-" {:dispatch {:args [:detail-dec]}}
      "basket" {:dispatch {:args [:basket]}}
      "basket+" {:dispatch {:args [:inc-basket-product]}}
      "basket-" {:dispatch {:args [:dec-basket-product]}}
      "to-order" {:dispatch {:args [:to-order]}}
      "request-location" {:dispatch {:args [:request-location]}}
      "change-payment-type" {:dispatch {:args [:change-payment-type]}}
      "change-comment" {:dispatch {:args [:change-comment]}}
      "create-order" {:dispatch {:args [:create-order]}}
      "invoice" {:dispatch {:args [:send-invoice]}}
      "cancel-invoice" {:dispatch {:args [:cancel-invoice]}}
      {:answer-callback {:callback_query_id (:id query)
                         :text              " "}})))


(d/register-event-handler!
  :callback
  callback-handler)
