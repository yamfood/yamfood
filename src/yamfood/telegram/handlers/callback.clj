(ns yamfood.telegram.handlers.callback
  (:require [yamfood.telegram.handlers.basket :as basket]
            [yamfood.telegram.handlers.order :as order]
            [yamfood.telegram.handlers.utils :as u]
            [yamfood.telegram.dispatcher :as d]))


(defn handle-callback
  [_ update]
  (let [query (:callback_query update)
        action (u/get-callback-action (:data query))]
    (cond
      (= action "want") {:dispatch {:args [:detail-want update]}}
      (= action "detail+") {:dispatch {:args [:detail-inc update]}}
      (= action "detail-") {:dispatch {:args [:detail-dec update]}}
      (= action "basket") {:dispatch {:args [:basket update]}}
      (= action "basket+") {:dispatch {:args [:inc-basket-product update]}}
      (= action "basket-") {:dispatch {:args [:dec-basket-product update]}}
      (= action "to-order") {:dispatch {:args [:to-order update]}}
      (= action "request-location") {:dispatch {:args [:request-location update]}}
      (= action "change-payment-type") {:dispatch {:args [:change-payment-type update]}}
      (= action "change-comment") {:dispatch {:args [:change-comment update]}}
      (= action "create-order") {:dispatch {:args [:create-order update]}}
      (= action "invoice") {:dispatch {:args [:send-invoice update]}}
      (= action "cancel-invoice") {:dispatch {:args [:cancel-invoice update]}})))


(d/register-event-handler!
  :callback
  handle-callback)
