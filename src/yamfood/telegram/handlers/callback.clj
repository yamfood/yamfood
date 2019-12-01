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
      (= action "request-location") (d/dispatch ctx [:request-location update])
      (= action "create-order") (d/dispatch ctx [:create-order update]))))


(d/register-event-handler!
  :callback
  handle-callback)
