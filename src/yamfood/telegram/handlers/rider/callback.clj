(ns yamfood.telegram.handlers.rider.callback
  (:require
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.dispatcher :as d]))


(defn callback-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        action (u/callback-action (:data query))]
    (case action
      "send-menu" {:dispatch {:args [:r/menu]}}
      {:answer-callback {:callback_query_id (:id query)
                         :text              " "}})))


(d/register-event-handler!
  :r/callback
  callback-handler)
