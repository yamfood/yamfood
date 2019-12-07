(ns yamfood.telegram.handlers.callback
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn callback-handler
  [_ update]
  (let [query (:callback_query update)
        action (u/callback-action (:data query))]
    (case action
      "want" {:dispatch {:args [:detail-want update]}}
      "detail+" {:dispatch {:args [:detail-inc update]}}
      "detail-" {:dispatch {:args [:detail-dec update]}}
      "basket" {:dispatch {:args [:basket update]}}
      "basket+" {:dispatch {:args [:inc-basket-product update]}}
      "basket-" {:dispatch {:args [:dec-basket-product update]}}
      "to-order" {:dispatch {:args [:to-order update]}}
      "request-location" {:dispatch {:args [:request-location update]}}
      "change-payment-type" {:dispatch {:args [:change-payment-type update]}}
      "change-comment" {:dispatch {:args [:change-comment update]}}
      "create-order" {:dispatch {:args [:create-order update]}}
      "invoice" {:dispatch {:args [:send-invoice update]}}
      "cancel-invoice" {:dispatch {:args [:cancel-invoice update]}}
      {:answer-callback {:callback_query_id (:id query)
                         :text " "}})))


(d/register-event-handler!
  :callback
  callback-handler)
