(ns yamfood.telegram.core
  (:require [morse.api :as t]
            [yamfood.core.products.core :as p]
            [environ.core :refer [env]]))


(def token (env :bot-token))

(defn handle-start
  [message]
  ((let [chat (:chat message)
         chat-id (:id chat)]
     (t/send-text
       token
       chat-id
       {:reply_markup {:reply_keyboard [{:text "test" :callback_data "test"}]}}
       "Welcome"))))


(defn process-message
  [message]
  (let [text (:text message)]
    (if (= text "/start") (handle-start message))))


(defn get-product-description
  [product]
  (str "Цена: " (:price product) ", Каллории: " (:energy product)))

(defn query-result-from-product
  [product]
  {:type "article"
   :id (:id product)
   :input_message_content {:message_text "test"}
   :title (:name product)
   :description (get-product-description product)
   :thumb_url (:photo product)})



(defn handle-inline-query
  [query]
  (t/answer-inline
    token
    (:id query)
    {:cache_time 0}
    (map query-result-from-product (p/get-all-products!))))


(defn log
  [message]
  (println (str "\n\n\n ### \n" message "\n\n\n")))


(defn process-updates
  [request]
  (log (:body request))
  (let [body (:body request)
        message (:message body)
        inline-query (:inline_query body)]
    (if message (process-message message))
    (if inline-query (handle-inline-query inline-query)))
  {:body "OK"})


;(t/set-webhook token "https://df9bd033.ngrok.io/updates")

