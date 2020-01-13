(ns yamfood.telegram.handlers.rider.inline
  (:require
    [yamfood.core.orders.core :as o]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn order-description
  [order]
  (str order))


(defn query-result-from-order
  [order]
  {:type                  "article"
   :id                    (:id order)
   :input_message_content {:message_text (:id order)}
   :title                 (str "Заказ №" (:id order))
   :description           (order-description order)})



(defn rider-inline-handler
  ([ctx]
   (let [update (:update ctx)
         inline_query (:inline_query update)
         query (:query inline_query)
         order-id (u/parse-int query)]
     (when (and
             (not (= query ""))
             order-id)
       {:run {:function   o/order-by-id!
              :args       [order-id {:products? false :totals? false}]
              :next-event :r/inline}})))
  ([ctx order]
   (let [update (:update ctx)]
     {:answer-inline
      {:inline-query-id (:id (:inline_query update))
       :options         {:cache_time 0}
       :results         [(query-result-from-order order)]}})))


(d/register-event-handler!
  :r/inline
  rider-inline-handler)
