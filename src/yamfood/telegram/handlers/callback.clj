(ns yamfood.telegram.handlers.callback
  (:require [yamfood.telegram.handlers.basket :as basket]
            [yamfood.telegram.handlers.order :as order]
            [yamfood.telegram.handlers.utils :as u]
            [yamfood.telegram.dispatcher :as d]))


; TODO: Rewrite this as dispatcher event handlers?
(defn handle-callback
  [ctx update]
  (let [query (:callback_query update)
        action (u/get-callback-action (:data query))]
    (cond
      (= action "want") (basket/handle-want ctx update)
      (= action "detail+") (basket/handle-inc ctx update)
      (= action "detail-") (basket/handle-dec ctx update)
      (= action "basket") (basket/handle-basket ctx update)
      (= action "basket+") (basket/handle-basket-inc ctx update)
      (= action "basket-") (basket/handle-basket-dec ctx update)
      (= action "to-order") (order/handle-to-order ctx update)
      (= action "request-location") (d/dispatch! ctx [:request-location update])
      (= action "change-payment-type") (d/dispatch! ctx [:change-payment-type update])
      (= action "change-comment") (d/dispatch! ctx [:change-comment update])
      (= action "create-order") (d/dispatch! ctx [:create-order update])
      (= action "invoice") (d/dispatch! ctx [:send-invoice update])
      (= action "cancel-invoice") (d/dispatch! ctx [:cancel-invoice update]))))


(d/register-event-handler!
  :callback
  handle-callback)
