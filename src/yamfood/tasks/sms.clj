(ns yamfood.tasks.sms
  (:require
    [yamfood.core.params.core :as p]
    [yamfood.core.sms.core :as sms-core]
    [yamfood.integrations.sms.core :as sms-api]))


(defn send-sms!
  [sms]
  (let [id (:id sms)
        sms {:message-id id
             :text       (:text sms)
             :phone      (:phone sms)}
        params (p/params!)]
    (if (= (sms-api/send-sms! params sms) 200)
      (sms-core/update! id {:is_sent true})
      (println "Error in sending sms"))))


(defn sms-daemon!
  []
  (try
    (println "Looking for new sms...")
    (doall
      (->> (sms-core/sms-to-send! 50)
           (pmap send-sms!)))))


(sms-daemon!)