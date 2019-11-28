(ns yamfood.telegram.handlers.order
  (:require [yamfood.telegram.dispatcher :as d]))


(def request-location-markup
  {:reply_markup
   {:resize_keyboard true
    :keyboard        [[{:text             "Отправить текущее положение"
                        :request_location true}]]}})


(defn handle-to-order
  [_ update]
  (let [query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))]
    {:delete-message {:chat-id    chat-id
                      :message-id message-id}
     :send-text      {:chat-id chat-id
                      :text    "Куда доставить?"
                      :options request-location-markup}}))


(def order-confirmation-markup
  {:inline_keyboard
   [[{:text "\uD83D\uDCCD" :callback_data "change-location"}
     {:text "\uD83D\uDCB5" :callback_data "change-pay"}
     {:text "\uD83D\uDCAC" :callback_data "change-comment"}]
    [{:text "\uD83E\uDDFA Корзина" :callback_data "basket"}]
    [{:text "✅ Подтвердить" :callback_data "nothing"}]]})


(defn make-order-text
  [basket-state]
  (format "*Детали вашего заказ:* \n\n\uD83D\uDCB0 %s сум \n\uD83D\uDCB5 %s \n\uD83D\uDCAC Без комментария \n\n\uD83D\uDCCD %s"
          "85 000"
          "Наличными"
          "60, 1st Akkurgan Passage, Mirzo Ulugbek district, Tashkent"))


(defn handle-location
  [_ update]
  (let [message (:message update)
        chat-id (:id (:from message))]
    {:send-text [{:chat-id chat-id
                  :text "Локация обновлена"
                  :options {:reply_markup {:remove_keyboard true}}}
                 {:chat-id chat-id
                  :text    (make-order-text {})
                  :options {:reply_markup order-confirmation-markup
                            :parse_mode "markdown"}}]}))


(d/register-event-handler!
  :location
  handle-location)


(handle-location {} {:update_id 435322755, :message {:message_id 9781, :from {:id 79225668, :is_bot false, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :language_code "ru"}, :chat {:id 79225668, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :type "private"}, :date 1574962444, :reply_to_message {:message_id 9779, :from {:id 488312680, :is_bot true, :first_name "Kensay", :username "kensaybot"}, :chat {:id 79225668, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :type "private"}, :date 1574962408, :text "Куда доставить?"}, :location {:latitude 32.020902, :longitude 34.740417}}})