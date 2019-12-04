(ns yamfood.telegram.handlers.order
  (:require [yamfood.core.users.core :as usr]
            [yamfood.core.orders.core :as ord]
            [yamfood.telegram.dispatcher :as d]
            [yamfood.core.baskets.core :as bsk]
            [yamfood.telegram.handlers.utils :as u]))


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


(defn pre-order-state
  [ctx update]
  (let [user (:user ctx)]
    {:core {:function    #(bsk/pre-order-state! (:basket_id user))
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
        (:location user) (pre-order-state ctx update)
        :else {:send-text {:chat-id chat-id
                           :text    "Куда доставить?"
                           :options request-location-markup}})
      {:delete-message {:chat-id    chat-id
                        :message-id message-id}})))


(def order-confirmation-markup
  {:inline_keyboard
   [[{:text u/location-emoji :callback_data "request-location"}
     {:text u/comment-emoji :callback_data "change-comment"}]
    [{:text (str u/basket-emoji " Корзина") :callback_data "basket"}]
    [{:text "✅ Подтвердить" :callback_data "create-order"}]]})


(defn make-order-text
  [order-state]
  (format (str "*Детали вашего заказа:* \n\n"
               u/money-emoji " %s сум \n"
               u/comment-emoji " `%s` \n\n"
               u/location-emoji " %s")
          (u/fmt-values (:total_cost (:basket order-state)))
          (:comment (:user order-state))
          "60, 1st Akkurgan Passage, Mirzo Ulugbek district, Tashkent"))


(defn send-order-detail
  [_ update order-state]
  (let [chat-id (u/chat-id update)]
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
     :core      [(:core (pre-order-state ctx update))
                 {:function #(usr/update-location!
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
    {:core            {:function #(ord/create-order-and-clear-basket!
                                    basket-id
                                    location
                                    comment)}
     :answer-callback {:callback_query_id (:id query)
                       :text              "Ваш заказ успешно создан! Мы будем держать вас в курсе его статуса."
                       :show_alert        true}
     :dispatch        {:args [:order-status update]}
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
     :delete-message {:chat-id    chat-id
                      :message-id message-id}}))


(defn order-products-text
  [products]
  (doall
    (map
      #(format (str u/food-emoji " %d x %s\n") (:count %) (:name %))
      products)))


(defn order-status-text
  [order]
  (format (str "*Заказ №1334:*\n\n"
               (apply str (order-products-text (:products order)))
               "\n"
               u/money-emoji " 53 200 сум (Не оплачено)\n\n"
               "Ожидает подтверждения оператором")))


(defn order-reply-markup
  [order]
  {:inline_keyboard [[{:text "Оплатить картой" :callback_data (str "invoice/" (:id order))}]]})


(defn product-price
  [product]
  {:label  (format "%d x %s" (:count product) (:name product))
   :amount (* (:price product) 100)})


(defn order-prices
  [order]
  (map product-price (:products order)))


(defn order-status
  ([ctx update]
   (order-status ctx update nil))
  ([ctx update order]
   (let [user (:user ctx)
         chat-id (u/chat-id update)]
     (cond
       (= order nil) {:core {:function    #(ord/user-active-order! (:id user))
                             :on-complete #(d/dispatch! ctx [:order-status update %])}}
       :else {:send-text {:chat-id chat-id
                          :text    (order-status-text order)
                          :options {:parse_mode   "markdown"
                                    :reply_markup (order-reply-markup order)}}}))))


(defn invoice-description
  [order]
  (format (str (apply str (order-products-text (:products order))))))


(defn invoice-reply-markup
  []
  {:inline_keyboard [[{:text "Оплатить" :pay true}]
                     [{:text "Отмена" :callback_data "cancel-invoice"}]]})


(defn send-invoice
  ([ctx update]
   (send-invoice ctx update nil))
  ([ctx update order]
   (let [user (:user ctx)
         chat-id (u/chat-id update)
         message-id (:message_id (:message (:callback_query update)))]
     (cond
       (= order nil) {:core {:function    #(ord/user-active-order! (:id user))
                             :on-complete #(d/dispatch! ctx [:send-invoice update %])}}
       :else {:send-invoice   {:chat-id     chat-id
                               :title       (str "Оплатить заказ №" (:id order))
                               :description (invoice-description order)
                               :payload     {}
                               :currency    "UZS"
                               :prices      (order-prices order)
                               :options     {:reply_markup (invoice-reply-markup)}}
              :delete-message {:chat-id    chat-id
                               :message-id message-id}}))))


(defn cancel-invoice
  [_ update]
  (let [query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))]
    {:dispatch       {:args [:order-status update]}
     :delete-message {:chat-id    chat-id
                      :message-id message-id}}))


(d/register-event-handler!
  :location
  handle-location)


(d/register-event-handler!
  :pre-order-state
  pre-order-state)


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


(d/register-event-handler!
  :order-status
  order-status)


(d/register-event-handler!
  :send-invoice
  send-invoice)


(d/register-event-handler!
  :cancel-invoice
  cancel-invoice)

