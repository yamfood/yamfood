(ns yamfood.telegram.handlers.order
  (:require
    [yamfood.core.users.core :as usr]
    [yamfood.core.orders.core :as ord]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.baskets.core :as bsk]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.core.regions.core :as regions]))


(def markup-for-request-location
  {:resize_keyboard true
   :keyboard        [[{:text             "Отправить текущее положение"
                       :request_location true}]]})


(defn request-location-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))]
    {:send-text      {:chat-id chat-id
                      :text    "Куда доставить?"
                      :options {:reply_markup markup-for-request-location}}
     :delete-message {:chat-id    chat-id
                      :message-id message-id}}))


(defn order-confirmation-state
  [ctx]
  (let [user (:user ctx)]
    {:run {:function   bsk/order-confirmation-state!
           :args       [(:basket_id user)]
           :next-event :send-order-detail}}))


(defn to-order-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (:id (:from query))
        user (:user ctx)
        message-id (:message_id (:message query))]
    (into
      (cond
        (:location user) (order-confirmation-state ctx)
        :else {:send-text {:chat-id chat-id
                           :text    "Куда доставить?"
                           :options {:reply_markup markup-for-request-location}}})
      {:delete-message {:chat-id    chat-id
                        :message-id message-id}})))


(def order-confirmation-markup
  {:inline_keyboard
   [[{:text u/location-emoji :callback_data "request-location"}
     {:text u/comment-emoji :callback_data "change-comment"}]
    [{:text (str u/basket-emoji " Корзина") :callback_data "basket"}]
    [{:text "✅ Подтвердить" :callback_data "create-order"}]]})


(defn pre-order-text
  [order-state]
  (format (str "*Детали вашего заказа:* \n\n"
               u/money-emoji " %s сум \n"
               u/comment-emoji " `%s` \n\n"
               u/location-emoji " %s")
          (u/fmt-values (:total_cost (:basket order-state)))
          (:comment (:user order-state))
          "60, 1st Akkurgan Passage, Mirzo Ulugbek district, Tashkent"))


(defn order-detail-handler
  [ctx order-state]
  (let [update (:update ctx)
        chat-id (u/chat-id update)]
    {:send-text {:chat-id chat-id
                 :text    (pre-order-text order-state)
                 :options {:reply_markup order-confirmation-markup
                           :parse_mode   "markdown"}}}))


(def invalid-location-markup
  {:inline_keyboard
   [[{:text "Карта обслуживания"
      :url  u/map-url}]
    [{:text          (str u/basket-emoji " Корзина")
      :callback_data "basket"}]]})


(defn location-handler
  ([ctx]
   (let [update (:update ctx)
         message (:message update)
         location (:location message)]
     {:run {:function   regions/region-by-location!
            :args       [(:longitude location)
                         (:latitude location)]
            :next-event :location}}))
  ([ctx region]
   (let [message (:message (:update ctx))
         chat-id (:id (:from message))]
     (if region
       {:dispatch {:args [:update-location]}}
       {:send-text [{:chat-id chat-id
                     :text    "Ждите..."
                     :options {:reply_markup {:remove_keyboard true}}}
                    {:chat-id chat-id
                     :text    "К сожалению, мы не обслуживаем данный регион"
                     :options {:reply_markup invalid-location-markup}}]}))))


(defn update-location
  [ctx]
  (let [update (:update ctx)
        message (:message update)
        chat-id (:id (:from message))
        location (:location message)]
    {:send-text {:chat-id chat-id
                 :text    "Локация обновлена"
                 :options {:reply_markup {:remove_keyboard true}}}
     :run       [(:run (order-confirmation-state ctx))
                 {:function usr/update-location!
                  :args     [(:id (:user ctx))
                             (:longitude location)
                             (:latitude location)]}]}))



(defn create-order-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))
        basket-id (:basket_id (:user ctx))
        location (:location (:user ctx))
        comment (:comment (:user ctx))]
    {:run             {:function ord/create-order-and-clear-basket!
                       :args     [basket-id location comment]}
     :answer-callback {:callback_query_id (:id query)
                       :text              "Ваш заказ успешно создан! Мы будем держать вас в курсе его статуса."
                       :show_alert        true}
     :dispatch        {:args [:order-status]}
     :delete-message  {:chat-id    chat-id
                       :message-id message-id}}))


(def write-comment-text "Напишите свой комментарий к заказу")
(defn change-comment-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
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
  ([ctx]
   {:run {:function   ord/user-active-order!
          :args       [(:id (:user ctx))]
          :next-event :order-status}})
  ([ctx order]
   (let [update (:update ctx)
         chat-id (u/chat-id update)]
     {:send-text {:chat-id chat-id
                  :text    (order-status-text order)
                  :options {:parse_mode   "markdown"
                            :reply_markup (order-reply-markup order)}}})))


(defn invoice-description
  [order]
  (format (str (apply str (order-products-text (:products order))))))


(def invoice-reply-markup
  {:inline_keyboard [[{:text "Оплатить" :pay true}]
                     [{:text "Отмена" :callback_data "cancel-invoice"}]]})


(defn send-invoice
  ([ctx]
   {:run {:function   ord/user-active-order!
          :args       [(:id (:user ctx))]
          :next-event :send-invoice}})
  ([ctx order]
   (let [update (:update ctx)
         chat-id (u/chat-id update)
         message-id (:message_id (:message (:callback_query update)))]
     {:send-invoice   {:chat-id     chat-id
                       :title       (str "Оплатить заказ №" (:id order))
                       :description (invoice-description order)
                       :payload     {}
                       :currency    "UZS"
                       :prices      (order-prices order)
                       :options     {:reply_markup invoice-reply-markup}}
      :delete-message {:chat-id    chat-id
                       :message-id message-id}})))


(defn cancel-invoice-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))]
    {:dispatch       {:args [:order-status]}
     :delete-message {:chat-id    chat-id
                      :message-id message-id}}))


(d/register-event-handler!
  :to-order
  to-order-handler)


(d/register-event-handler!
  :location
  location-handler)


(d/register-event-handler!
  :update-location
  update-location)


(d/register-event-handler!
  :order-confirmation-state
  order-confirmation-state)


(d/register-event-handler!
  :request-location
  request-location-handler)


(d/register-event-handler!
  :send-order-detail
  order-detail-handler)


(d/register-event-handler!
  :create-order
  create-order-handler)


(d/register-event-handler!
  :change-comment
  change-comment-handler)


(d/register-event-handler!
  :order-status
  order-status)


(d/register-event-handler!
  :send-invoice
  send-invoice)


(d/register-event-handler!
  :cancel-invoice
  cancel-invoice-handler)
