(ns yamfood.telegram.core
  (:require [morse.api :as t]
            [yamfood.core.products.core :as products]
            [yamfood.core.users.core :as users]
            [yamfood.telegram.inline :as inline]
            [yamfood.telegram.start :as start]
            [clojure.data.json :as json]
            [environ.core :refer [env]]))


(def token (env :bot-token))

(defn get-product-caption
  [product]
  (format "*%s* \n\n *Цена:* %,d сум."
          (:name product)
          (:price product)))


(defn get-product-detail-options
  [product]
  {:caption      (get-product-caption product)
   :parse_mode   "markdown"
   :reply_markup (json/write-str                            ; Because morse send photo by multipart and not convert it.
                   {:inline_keyboard
                    [[{:text                             "Еще!"
                       :switch_inline_query_current_chat ""}]]})})


(defn handle-text
  [ctx message]
  (let [product (products/get-product-by-name! (:text message))
        chat (:chat message)
        chat-id (:id chat)]
    (if product
      (t/send-photo (:token ctx) chat-id
                    (get-product-detail-options product)
                    (:photo product))

      (t/send-text (:token ctx) chat-id
                   "Если у вас возникли какие-то вопросы обратитесь к @kensay."))))


(defn process-message
  [ctx message]
  (let [text (:text message)
        contact (:contact message)]
    (cond
      (= text "/start") (start/handle-start ctx message)
      contact (start/handle-contact ctx message)
      :else (handle-text ctx message))))


(defn log
  [message]
  (println (str "\n\n\n ### \n" message "\n\n\n")))


(defn get-tid-from-update
  [update]
  (let [message (:message update)]
    (:id (:from message))))


(defn build-ctx
  [update]
  {:token token
   :user  (users/get-user-by-tid! (get-tid-from-update update))})


(defn process-updates
  [request]
  (log (:body request))
  (let [update (:body request)
        message (:message update)
        inline-query (:inline_query update)
        ctx (build-ctx update)]
    (if message
      (process-message ctx message))
    (if inline-query
      (inline/handle-inline-query ctx inline-query)))
  {:body "OK"})


;(process-updates {:body {:update_id 435322226, :message {:message_id 9464, :from {:id 79225668, :is_bot false, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :language_code "en"}, :chat {:id 79225668, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :type "private"}, :date 1572887003, :contact {:phone_number "998909296339", :first_name "Рустам", :last_name "Бабаджанов", :user_id 79225668}}}})
;(t/set-webhook token "https://c00eb9e9.ngrok.io/updates")

