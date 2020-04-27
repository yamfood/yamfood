(ns yamfood.integrations.sms.core
  (:require
    [yamfood.integrations.sms.playmobile :as pm]))


(def send-sms! pm/send!)
