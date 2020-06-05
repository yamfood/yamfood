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
        params (p/params!)
        result (sms-api/send-sms! params sms)]
    (if (= result 200)
      (sms-core/update! id {:is_sent true})
      (sms-core/update! id {:error (str result)}))))


(defn sms-daemon!
  []
  (try
    (println "Looking for new sms...")
    (doall
      (->> (sms-core/sms-to-send! 50)
           (pmap send-sms!)))
    (catch Exception e
      (println e))))
