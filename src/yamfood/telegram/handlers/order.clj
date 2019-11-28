(ns yamfood.telegram.handlers.order
  (:require [yamfood.telegram.dispatcher :as d]
            [yamfood.core.users.core :as users]))


(def location-emoji "\uD83D\uDCCD")
(def payment-emoji "\uD83D\uDCB5")
(def money-emoji "\uD83D\uDCB0")
(def comment-emoji "\uD83D\uDCAC")
(def basket-emoji "\uD83E\uDDFA")


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
    {:send-text      {:chat-id chat-id
                      :text    "Куда доставить?"
                      :options request-location-markup}
     :delete-message {:chat-id    chat-id
                      :message-id message-id}}))


(defn make-order-state
  [ctx update]
  {:core {:function    hash-map
          :on-complete #(d/dispatch
                          ctx
                          [:send-order-detail update %])}})


(defn handle-to-order
  [ctx update]
  (let [query (:callback_query update)
        chat-id (:id (:from query))
        user (:user ctx)
        message-id (:message_id (:message query))]
    (into
      (cond
        (:location user) (make-order-state ctx update)
        :else {:send-text {:chat-id chat-id
                           :text    "Куда доставить?"
                           :options request-location-markup}})
      {:delete-message {:chat-id    chat-id
                        :message-id message-id}})))


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


(defn get-chat-id-from-update
  "Use when you don't know input update type"
  [update]
  (let [message (:message update)
        query (:callback_query update)]
    (cond
      message (:id (:from message))
      query (:id (:from query)))))


(defn send-order-detail
  [_ update order-state]
  (let [chat-id (get-chat-id-from-update update)]
    {:send-text {:chat-id chat-id
                 :text    (make-order-text order-state)
                 :options {:reply_markup order-confirmation-markup
                           :parse_mode   "markdown"}}}))


(defn handle-location
  [ctx update]
  (let [message (:message update)
        chat-id (:id (:from message))
        location (:location message)]
    {:send-text {:chat-id chat-id
                 :text    "Локация обновлена"
                 :options {:reply_markup {:remove_keyboard true}}}
     :core      [(:core (make-order-state ctx update))
                 {:function #(users/update-location!
                               (:id (:user ctx))
                               (:longitude location)
                               (:latitude location))}]}))


(d/register-event-handler!
  :location
  handle-location)


(d/register-event-handler!
  :request-location
  request-location)


(d/register-event-handler!
  :send-order-detail
  send-order-detail)


(send-order-detail {} {:update_id 435322822, :callback_query {:id "340271653891766996", :from {:id 79225668, :is_bot false, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :language_code "ru"}, :message {:message_id 9911, :from {:id 488312680, :is_bot true, :first_name "Kensay", :username "kensaybot"}, :chat {:id 79225668, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :type "private"}, :date 1574969847, :text "Ваша корзина:", :reply_markup {:inline_keyboard [[{:text "🥗 Рисовая каша с ежевикой x 1", :callback_data "nothing"}] [{:text "-", :callback_data "basket-/2"} {:text "13 800 сум", :callback_data "nothing"} {:text "+", :callback_data "basket+/2"}] [{:text "🥗 Скрембл с авокадо и помидорами x 3", :callback_data "nothing"}] [{:text "-", :callback_data "basket-/3"} {:text "66 000 сум", :callback_data "nothing"} {:text "+", :callback_data "basket+/3"}] [{:text "🥗 Сырники со сметаной и джемом x 2", :callback_data "nothing"}] [{:text "-", :callback_data "basket-/4"} {:text "30 000 сум", :callback_data "nothing"} {:text "+", :callback_data "basket+/4"}] [{:text "🥗 Свежесваренный кофе x 1", :callback_data "nothing"}] [{:text "-", :callback_data "basket-/9"} {:text "11 000 сум", :callback_data "nothing"} {:text "+", :callback_data "basket+/9"}] [{:text "🥗 Яблочный фреш x 1", :callback_data "nothing"}] [{:text "-", :callback_data "basket-/10"} {:text "9 900 сум", :callback_data "nothing"} {:text "+", :callback_data "basket+/10"}] [{:text "Еще!", :switch_inline_query_current_chat ""}] [{:text "💰 130 700 сум 🔋 3 380 кКал.", :callback_data "nothing"}] [{:text "✅ Далее", :callback_data "to-order"}]]}}, :chat_instance "4402156230761928760", :data "to-order"}} {})