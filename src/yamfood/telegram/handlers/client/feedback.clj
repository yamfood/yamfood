(ns yamfood.telegram.handlers.client.feedback
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn feedback-handler
  [ctx]
  (let [query (:callback_query (:update ctx))
        params (u/callback-params (:data query))
        order-id (u/parse-int (first params))
        rate (u/parse-int (second params))]
    {:run      {:function #(println %1 %2)
                :args     [order-id rate]}
     :dispatch {:args [:c/menu]}}))


(d/register-event-handler!
  :c/feedback
  feedback-handler)
