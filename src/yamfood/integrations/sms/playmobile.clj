(ns yamfood.integrations.sms.playmobile
  (:require
    [yamfood.utils :as u]
    [clj-http.client :as http]
    [clojure.data.json :as json]))


(defn prepare
  [sms]
  {:messages
   [{:recipient  (str (:phone sms))
     :message-id (str (:message-id sms))
     :sms        {:originator "3700"
                  :content    {:text (:text sms)}}}]})


(defn send!
  [params sms]
  (let [login (:playmobile-login params)
        password (:playmobile-password params)
        url (:playmobile-url params)
        sms (prepare sms)
        response (http/post url {:body                  (json/write-str sms)
                                 :content-type          :json
                                 :basic-auth            [login password]
                                 :throw-entire-message? true})]
    (:status response)))
