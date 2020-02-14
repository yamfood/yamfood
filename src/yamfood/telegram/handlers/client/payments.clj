(ns yamfood.telegram.handlers.client.payments
  (:require [yamfood.telegram.dispatcher :as d]))


; {:update_id 220544491, :pre_checkout_query {:id "340271656338300632", :from {:id 79225668, :is_bot false, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :language_code "ru"}, :currency "UZS", :total_amount 1500000, :invoice_payload "{}"}}
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



;{:update_id 220544499, :message {:message_id 10393, :from {:id 79225668, :is_bot false, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :language_code "ru"}, :chat {:id 79225668, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :type "private"}, :date 1581667791, :successful_payment {:currency "UZS", :total_amount 1500000, :invoice_payload "{}", :telegram_payment_charge_id "_", :provider_payment_charge_id "1581667790843"}}}