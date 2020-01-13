(ns yamfood.telegram.handlers.rider.core
  (:require
    [environ.core :refer [env]]
    [yamfood.core.riders.core :as r]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn build-ctx!
  [update]
  {:token  (env :rider-bot-token)
   :update update
   :rider  (r/rider-by-tid! (u/tid-from-update update))})


(defn process-message
  [ctx update]
  (let [message (:message update)
        text (:text message)]
    (cond
      (= text "/start") (d/dispatch! ctx [:r/menu])
      text (d/dispatch! ctx [:r/text]))))


(defn rider-update-handler!
  [update]
  (let [message (:message update)
        ctx (build-ctx! update)]
    (when (:rider ctx)
      (if message
        (process-message ctx update)))))
