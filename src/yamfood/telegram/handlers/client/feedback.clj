(ns yamfood.telegram.handlers.client.feedback
  (:require
    [yamfood.core.orders.core :as o]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn feedback-handler
  [ctx]
  (let [query (:callback_query (:update ctx))
        params (u/callback-params (:data query))
        order-id (u/parse-int (first params))
        rate (second params)]
    {:run      {:function o/update!
                :args     [order-id {:rate rate}]}
     :dispatch {:args [:c/menu]}}))


(d/register-event-handler!
  :c/feedback
  feedback-handler)
