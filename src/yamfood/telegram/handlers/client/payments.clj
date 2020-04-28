(ns yamfood.telegram.handlers.client.payments
  (:require
    [clojure.data.json :as json]
    [yamfood.core.orders.core :as o]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.baskets.core :as bsk]))


(defn pre-checkout-query-handler
  [ctx]
  (let [update (:update ctx)
        query (:pre_checkout_query update)]
    {:answer-pre-checkout-query {:pre_checkout_query_id (:id query)
                                 :ok                    true}}))


;{:update_id 220544755, :message {:successful_payment {:currency "UZS", :total_amount 2200000, :invoice_payload "{\"order_id\":19}", :telegram_payment_charge_id "_", :provider_payment_charge_id "1582276005878"}}}
(defn successful-payment-handler
  [ctx]
  (let [update (:update ctx)
        payment (:successful_payment (:message update))
        payload (json/read-str (:invoice_payload payment) :key-fn keyword)
        payment-id (:provider_payment_charge_id payment)
        order-id (:order_id payload)]
    {:run      [{:function o/pay-order!
                 :args     [order-id payment-id]}
                {:function bsk/clear-basket!
                 :args     [(:basket_id (:client ctx))]}]
     :dispatch {:args [:c/active-order order-id]}}))


(d/register-event-handler!
  :c/pre-checkout
  pre-checkout-query-handler)


(d/register-event-handler!
  :c/successful-payment
  successful-payment-handler)
