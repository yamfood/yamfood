(ns yamfood.telegram.methods
  (:require
    [clj-http.client :as http]
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


(defn edit-caption
  "Use this method to edit captions of messages
  (https://core.telegram.org/bots/api#editmessagecaption)"
  ([token chat-id message-id caption] (edit-caption token chat-id message-id {} caption))
  ([token chat-id message-id options caption]
   (let [url (str base-url token "/editMessageCaption")
         query (into {:chat_id chat-id :caption caption :message_id message-id} options)
         resp (http/post url {:content-type :json
                              :as           :json
                              :form-params  query})]
     (-> resp :body))))


(defn send-location
  "Sends location to the chat
  (https://core.telegram.org/bots/api#sendlocation)"
  ([token chat-id longitude latitude] (send-location token chat-id longitude latitude {}))
  ([token chat-id longitude latitude options]
   (let [url (str base-url token "/sendLocation")
         query (into {:chat_id chat-id :longitude longitude :latitude latitude} options)
         resp (http/post url {:content-type :json
                              :as           :json
                              :form-params  query})]
     (-> resp :body))))


(defn send-animation
  "Sends animation to the chat
  (https://core.telegram.org/bots/api#sendanimation)"
  ([token chat-id animation] (send-animation token chat-id animation {}))
  ([token chat-id animation options]
   (let [url (str base-url token "/sendAnimation")
         query (into {:chat_id chat-id :animation animation} options)
         resp (http/post url {:content-type :json
                              :as           :json
                              :form-params  query})]
     (-> resp :body))))


(defn send-invoice
  "Sends invoice to the chat
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


(defn answer-pre-checkout-query
  "Once the user has confirmed their payment and shipping details,
   the Bot API sends the final confirmation in the form of an Update
   with the field pre_checkout_query.
  (https://core.telegram.org/bots/api#answerPreCheckoutQuery)"
  ([token pre_checkout_query_id ok options]
   (let [url (str base-url token "/answerPreCheckoutQuery")
         query (into {:pre_checkout_query_id pre_checkout_query_id
                      :ok                    ok}
                     options)
         resp (http/post url {:content-type :json
                              :as           :json
                              :form-params  query})]
     (-> resp :body))))
