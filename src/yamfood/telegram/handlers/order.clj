(ns yamfood.telegram.handlers.order
  (:require [yamfood.telegram.dispatcher :as d]
            [yamfood.core.users.core :as users]))


(def request-location-markup
  {:reply_markup
   {:resize_keyboard true
    :keyboard        [[{:text             "Отправить текущее положение"
                        :request_location true}]]}})


(defn request-location
  [_ update]
  (let [query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))]
    {:delete-message {:chat-id    chat-id
                      :message-id message-id}
     :send-text      {:chat-id chat-id
                      :text    "Куда доставить?"
                      :options request-location-markup}}))



(defn handle-to-order
  [_ update]
  (let [query (:callback_query update)
        chat-id (:id (:from query))]
    {:send-text {:chat-id chat-id
                 :text    "Куда доставить?"
                 :options request-location-markup}}))


(def location-emoji "\uD83D\uDCCD")
(def payment-emoji "\uD83D\uDCB5")
(def money-emoji "\uD83D\uDCB0")
(def comment-emoji "\uD83D\uDCAC")
(def basket-emoji "\uD83E\uDDFA")

(def order-confirmation-markup
  {:inline_keyboard
   [[{:text location-emoji :callback_data "request-location"}
     {:text payment-emoji :callback_data "change-payment-type"}
     {:text comment-emoji :callback_data "change-comment"}]
    [{:text (str basket-emoji " Корзина") :callback_data "basket"}]
    [{:text "✅ Подтвердить" :callback_data "nothing"}]]})


(defn make-order-text
  [order-state]
  (format (str "*Детали вашего заказа:* \n\n"
               money-emoji " %s сум \n"
               payment-emoji " %s \n"
               comment-emoji " Без комментария \n\n"
               location-emoji " %s")
          "85 000"
          "Наличными"
          "60, 1st Akkurgan Passage, Mirzo Ulugbek district, Tashkent"))


(defn handle-location
  [ctx update]
  (let [message (:message update)
        chat-id (:id (:from message))
        location (:location message)]
    {:send-text [{:chat-id chat-id
                  :text    "Локация обновлена"
                  :options {:reply_markup {:remove_keyboard true}}}
                 {:chat-id chat-id
                  :text    (make-order-text {})
                  :options {:reply_markup order-confirmation-markup
                            :parse_mode   "markdown"}}]
     :core      {:function #(users/update-location!
                              (:id (:user ctx))
                              (:longitude location)
                              (:latitude location))}}))


(d/register-event-handler!
  :location
  handle-location)


(d/register-event-handler!
  :request-location
  request-location)
