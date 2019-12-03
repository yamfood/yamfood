(ns yamfood.telegram.handlers.order
  (:require [yamfood.telegram.dispatcher :as d]
            [yamfood.core.users.core :as users]
            [yamfood.telegram.handlers.utils :as u]
            [yamfood.core.orders.core :as orders]
            [yamfood.core.baskets.core :as baskets]))


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
  (let [user (:user ctx)]
    {:core {:function    #(baskets/make-order-state! (:basket_id user))
            :on-complete #(d/dispatch!
                            ctx
                            [:send-order-detail update %])}}))


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
   [[{:text u/location-emoji :callback_data "request-location"}
     {:text u/payment-emoji :callback_data "change-payment-type"}
     {:text u/comment-emoji :callback_data "change-comment"}]
    [{:text (str u/basket-emoji " Корзина") :callback_data "basket"}]
    [{:text "✅ Подтвердить" :callback_data "create-order"}]]})


(defn make-order-text
  [order-state]
  (format (str "*Детали вашего заказа:* \n\n"
               u/money-emoji " %s сум \n"
               u/payment-emoji " %s \n"
               u/comment-emoji " `%s` \n\n"
               u/location-emoji " %s")
          (u/fmt-values (:total_cost (:basket order-state)))
          "Наличными"
          (:comment (:user order-state))
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


(defn handle-create-order
  [ctx update]
  (let [query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))
        basket-id (:basket_id (:user ctx))
        location (:location (:user ctx))
        comment "test"]
    {:core            {:function #(orders/create-order-and-clear-basket!
                                    basket-id
                                    location
                                    comment)}
     :answer-callback {:callback_query_id (:id query)
                       :text              "Ваш заказ успешно создан! Мы будем держать вас в курсе его статуса."
                       :show_alert        true}
     :delete-message  {:chat-id    chat-id
                       :message-id message-id}}))


(defn handle-change-payment-type
  [_ update]
  {:answer-callback {:callback_query_id (:id (:callback_query update))
                     :text              "К сожалению, на данный момент мы принимает оплату только наличными :("
                     :show_alert        true}})


(def write-comment-text "Напишите свой комментарий к заказу")
(defn handle-change-comment
  [_ update]
  (let [query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))]
    {:send-text      {:chat-id chat-id
                      :text    write-comment-text
                      :options {:reply_markup {:force_reply true}}}
     :delete-message {:chat-id chat-id
                      :message-id message-id}}))



(d/register-event-handler!
  :location
  handle-location)


(d/register-event-handler!
  :make-order-state
  make-order-state)


(d/register-event-handler!
  :request-location
  request-location)


(d/register-event-handler!
  :send-order-detail
  send-order-detail)


(d/register-event-handler!
  :create-order
  handle-create-order)


(d/register-event-handler!
  :change-payment-type
  handle-change-payment-type)


(d/register-event-handler!
  :change-comment
  handle-change-comment)