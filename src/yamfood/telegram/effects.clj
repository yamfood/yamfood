(ns yamfood.telegram.effects
  (:require [yamfood.telegram.dispatcher :as d]
            [morse.api :as t]
            [environ.core :refer [env]]))


(d/register-effect-handler!
  :answer-inline
  (fn [ctx effect]
    (t/answer-inline
      (:token ctx)
      (:inline-query-id effect)
      (:options effect)
      (:results effect))))


(d/register-effect-handler!
  :send-text
  (fn [ctx effect]
    (t/send-text
      (:token ctx)
      (:chat-id effect)
      (:options effect)
      (:text effect))))


(d/register-effect-handler!
  :send-photo
  (fn [ctx effect]
    (t/send-photo
      (:token ctx)
      (:chat-id effect)
      (:options effect)
      (:photo effect))))
