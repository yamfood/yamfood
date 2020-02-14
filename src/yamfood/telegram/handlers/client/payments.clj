(ns yamfood.telegram.handlers.client.payments
  (:require
    [yamfood.telegram.dispatcher :as d]))


(defn pre-checkout-query-handler
  [ctx]
  (let [update (:update ctx)
        query (:pre_checkout_query update)]
    {:answer-pre-checkout-query {:pre_checkout_query_id (:id query)
                                 :ok                    true}}))


(defn successful-payment-handler
  [ctx]
  {})


(d/register-event-handler!
  :c/pre-checkout
  pre-checkout-query-handler)


(d/register-event-handler!
  :c/successful-payment
  successful-payment-handler)
