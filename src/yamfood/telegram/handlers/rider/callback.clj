(ns yamfood.telegram.handlers.rider.callback
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn callback-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        action (u/callback-action (:data query))]
    (case action
      "send-menu" {:dispatch {:args [:r/menu]}}
      "assign" {:dispatch {:args [:r/assign]}}
      "decline" {:dispatch {:args [:r/decline]}}
      "refresh-menu" {:dispatch {:args [:r/refresh-menu]}}
      "order-detail" {:dispatch {:args [:r/order-detail]}}
      "order-products" {:dispatch {:args [:r/order-products]}}
      "finish-order" {:dispatch {:args [:r/finish-order]}}
      "cancel-order" {:dispatch {:args [:r/cancel-order]}}
      {:answer-callback {:callback_query_id (:id query)
                         :text              " "}})))


(d/register-event-handler!
  :r/callback
  callback-handler)
