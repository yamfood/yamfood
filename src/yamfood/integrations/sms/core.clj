(ns yamfood.integrations.sms.core
  (:require
    [yamfood.integrations.sms.playmobile :as pm]))


(defn send-sms!
  [params sms]
  (pm/send! params sms))
