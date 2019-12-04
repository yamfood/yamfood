(ns yamfood.telegram.core
  (:require [yamfood.telegram.events]
            [yamfood.telegram.effects]
            [environ.core :refer [env]]
            [yamfood.core.users.core :as users]
            [yamfood.telegram.dispatcher :as d]))


(defn get-tid-from-update                                   ; TODO: Make it work with all updates!
  [update]
  (let [message (:message update)
        callback (:callback_query update)
        inline (:inline_query update)]
    (cond
      message (:id (:from message))
      callback (:id (:from callback))
      inline (:id (:from inline)))))


(defn build-ctx!
  [update]
  {:token          (env :bot-token)
   :payments-token (env :payments-token)
   :user           (users/user-with-tid! (get-tid-from-update update))})


(defn process-message
  [ctx update]
  (let [message (:message update)
        text (:text message)
        contact (:contact message)
        location (:location message)
        reply-to (:reply_to_message message)]
    (cond
      (= text "/start") (d/dispatch! ctx [:start update])
      location (d/dispatch! ctx [:location update])
      contact (d/dispatch! ctx [:contact update])
      reply-to (d/dispatch! ctx [:reply update])
      text (d/dispatch! ctx [:text message]))))


(defn handle-update!
  [update]
  (let [message (:message update)
        inline-query (:inline_query update)
        callback-query (:callback_query update)
        ctx (build-ctx! update)]
    (if message
      (process-message ctx update))
    (if inline-query
      (d/dispatch! ctx [:inline update]))
    (if callback-query
      (d/dispatch! ctx [:callback update]))))


(defn telegram-handler
  [request]
  (try
    (handle-update! (:body request))
    (catch Exception e
      (println
        (format
          "\n\n %s \n\n"
          {:update (:body request) :error e}))))
  {:body "OK"})

;(morse.api/set-webhook (env :bot-token) "https://d4d815c9.ngrok.io/updates")
(def upd {:update_id 435323018, :callback_query {:id "340271655783135633", :from {:id 79225668, :is_bot false, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :language_code "ru"}, :message {:message_id 10091, :from {:id 488312680, :is_bot true, :first_name "Kensay", :username "kensaybot"}, :chat {:id 79225668, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :type "private"}, :date 1575467316, :text "Детали вашего заказа: \n\n💰 64 600 сум \n💬 Побольше чего нибудь \n\n📍 60, 1st Akkurgan Passage, Mirzo Ulugbek district, Tashkent", :entities [{:offset 0, :length 21, :type "bold"} {:offset 42, :length 20, :type "code"}], :reply_markup {:inline_keyboard [[{:text "📍", :callback_data "request-location"} {:text "💬", :callback_data "change-comment"}] [{:text "🧺 Корзина", :callback_data "basket"}] [{:text "✅ Подтвердить", :callback_data "create-order"}]]}}, :chat_instance "4402156230761928760", :data "create-order"}})
(d/dispatch! (build-ctx! upd) [:order-status upd])
