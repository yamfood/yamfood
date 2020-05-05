(ns yamfood.telegram.handlers.rider.core
  (:require
    [environ.core :refer [env]]
    [yamfood.core.riders.core :as r]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.params.core :as params]
    [yamfood.telegram.handlers.utils :as u]))


(defn build-ctx!
  [update]
  {:token  (env :rider-bot-token)
   :update update
   :params (params/params!)
   :rider  (r/rider-by-tid! (u/tid-from-update update))})


(defn process-message
  [ctx update]
  (let [rider (:rider ctx)
        message (:message update)
        contact (:contact message)
        text (:text message)]
    (if rider
      (cond
        (= text "/start") (d/dispatch! ctx [:r/menu])
        contact (d/dispatch! ctx [:r/contact])
        text (d/dispatch! ctx [:r/text]))
      (d/dispatch! ctx [:r/menu]))))


(defn rider-update-handler!
  [update]
  (let [message (:message update)
        inline_query (:inline_query update)
        callback_query (:callback_query update)
        ctx (build-ctx! update)]
    (cond
      message (process-message ctx update)
      callback_query (d/dispatch! ctx [:r/callback])
      inline_query (d/dispatch! ctx [:r/inline]))))
