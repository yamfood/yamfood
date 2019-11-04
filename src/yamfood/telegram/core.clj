(ns yamfood.telegram.core
  (:require [morse.api :as t]
            [yamfood.core.products.core :as p]
            [yamfood.telegram.inline :as inline]
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
  {:caption (get-product-caption product)
   :parse_mode "markdown"
   :reply_markup (json/write-str ; Because morse send photo by multipart and not convert it.
                   {:inline_keyboard
                    [[{:text "Еще!"
                       :switch_inline_query_current_chat ""}]]})})


(defn handle-text
  [message]
  (let [product (p/get-product-by-name! (:text message))
        chat (:chat message)
        chat-id (:id chat)]
    (t/send-photo token chat-id
                  (get-product-detail-options product)
                  (:photo product))))


(defn handle-start
  [message]
  ((let [chat (:chat message)
         chat-id (:id chat)]
     (t/send-text
       token
       chat-id
       {:reply_markup {:inline_keyboard [[{:text "test"
                                           :switch_inline_query_current_chat ""}]]}}
       "Welcome"))))


(defn process-message
  [message]
  (let [text (:text message)]
    (if (= text "/start")
      (handle-start message))
    (handle-text message)))


(defn log
  [message]
  (println (str "\n\n\n ### \n" message "\n\n\n")))


(defn process-updates
  [request]
  (log (:body request))
  (let [update (:body request)
        message (:message update)
        inline-query (:inline_query update)]
    (if message
      (process-message message))
    (if inline-query
      (inline/handle-inline-query token inline-query)))
  {:body "OK"})


;(process-updates {:body {:update_id 919386259, :inline_query {:id "340271655998180564", :from {:id 79225668, :is_bot false, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :language_code "ru"}, :query "", :offset ""}}})
;(t/set-webhook token "https://df9bd033.ngrok.io/updates")

