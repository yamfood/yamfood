(ns yamfood.telegram.methods
  (:require [clj-http.client :as http]
            [environ.core :refer [env]]))


(def base-url "https://api.telegram.org/bot")


(defn edit-reply-markup
  "Edits only the reply markup of message
  (https://core.telegram.org/bots/api#editmessagereplymarkup)"
  ([token chat-id message-id reply-markup] (edit-reply-markup token chat-id message-id {} reply-markup))
  ([token chat-id message-id options reply-markup]
   (let [url (str base-url token "/editMessageReplyMarkup")
         query (into {:chat_id chat-id :reply_markup reply-markup :message_id message-id} options)
         resp (http/post url {:content-type :json
                              :as           :json
                              :form-params  query})]
     (-> resp :body))))


(defn send-invoice
  "Edits only the reply markup of message
  (https://core.telegram.org/bots/api#editmessagereplymarkup)"
  ([token provider-token
    chat-id title description
    payload currency prices options]
   (send-invoice token provider-token "test"
                 chat-id title description
                 payload currency prices options))
  ([token provider-token
    start_parameter chat-id
    title description payload
    currency prices options]
   (let [url (str base-url token "/sendInvoice")
         query (into {:chat_id         chat-id
                      :provider_token  provider-token
                      :start_parameter start_parameter
                      :title           title
                      :description     description
                      :payload         payload
                      :currency        currency
                      :prices          prices}
                     options)
         resp (http/post url {:content-type :json
                              :as           :json
                              :form-params  query})]
     (-> resp :body))))


;(try
;  (send-invoice
;    "987870891:AAFa3fERJBlQcdagHX3U8lcgvcpV22YO8oY"
;    "371317599:TEST:79225668"
;    57248115
;    "test"
;    "test"
;    {:test "test"}
;    "UZS"
;    [{:label "Сумма" :amount 50000}])
;  (catch Exception e
;    (println e)))

