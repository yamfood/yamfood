(ns yamfood.telegram.handlers.client.order
  (:require
    [yamfood.core.orders.core :as o]
    [yamfood.core.users.core :as usr]
    [yamfood.core.orders.core :as ord]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.baskets.core :as bsk]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.handlers.client.core :as c]))


(defn order-confirmation-state
  [ctx]
  (let [user (:user ctx)]
    {:run {:function   bsk/order-confirmation-state!
           :args       [(:basket_id user)]
           :next-event :c/send-order-detail}}))


(defn to-order-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (:id (:from query))
        user (:user ctx)
        message-id (:message_id (:message query))]
    (into
      (cond
        (:location (:payload user)) {:dispatch {:args [:c/order-confirmation-state]}}
        :else {:dispatch {:args [:c/request-location]}})
      {:delete-message {:chat-id    chat-id
                        :message-id message-id}
       :run            {:function usr/update-payload!
                        :args     [(:id user)
                                   (assoc (:payload user) :step u/order-confirmation-step)]}})))


(defn order-confirmation-markup
  [order-state]
  (let [payment (get-in order-state [:user :payload :payment])]
    {:inline_keyboard
     [[{:text u/location-emoji :callback_data "request-location"}
       (cond
         (= payment u/card-payment) {:text u/card-emoji :callback_data "switch-payment-type"}
         :else {:text u/cash-emoji :callback_data "switch-payment-type"})
       {:text u/comment-emoji :callback_data "change-comment"}]
      [{:text (str u/basket-emoji " Корзина") :callback_data "basket"}]
      [{:text "✅ Подтвердить" :callback_data "create-order"}]]}))


(defn pre-order-text
  [order-state]
  (format (str "*Детали вашего заказа:* \n\n"
               u/money-emoji " %s сум (%s)\n"
               u/comment-emoji " `%s` \n\n"
               u/location-emoji " %s")
          (u/fmt-values (:total_cost (:basket order-state)))
          (or (get-in order-state [:user :payload :payment :label]) "Наличными")
          (or (:comment (:payload (:user order-state))) "Пусто...")
          (u/text-from-address
            (get-in order-state [:user :payload :location :address]))))


(defn order-detail-handler
  [ctx order-state]
  (let [update (:update ctx)
        chat-id (u/chat-id update)]
    {:send-text {:chat-id chat-id
                 :text    (pre-order-text order-state)
                 :options {:reply_markup (order-confirmation-markup order-state)
                           :parse_mode   "markdown"}}}))


(defn update-order-confirmation-handler
  ([ctx]
   (let [user (:user ctx)]
     {:run {:function   bsk/order-confirmation-state!
            :args       [(:basket_id user)]
            :next-event :c/update-order-confirmation}}))
  ([ctx order-state]
   (let [query (:callback_query (:update ctx))
         chat-id (:id (:from query))
         message-id (:message_id (:message query))]
     {:edit-message {:chat-id    chat-id
                     :message-id message-id
                     :text       (pre-order-text order-state)
                     :options    {:reply_markup (order-confirmation-markup order-state)
                                  :parse_mode   "markdown"}}})))


(defn switch-payment-type-handler
  [ctx]
  (let [query (:callback_query (:update ctx))
        user (:user ctx)
        payload (:payload user)
        payment (:payment payload)
        new-payment (cond
                      (or (= payment nil)
                          (= payment u/cash-payment)) u/card-payment
                      (= payment u/card-payment) u/cash-payment)]
    {:run             {:function usr/update-payload!
                       :args     [(:id user) (assoc payload :payment new-payment)]}
     :dispatch        {:args        [:c/update-order-confirmation]
                       :rebuild-ctx {:function c/build-ctx!
                                     :update   (:update ctx)}}
     :answer-callback {:callback_query_id (:id query)
                       :text              " "}}))


(defn create-order-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))
        basket-id (:basket_id (:user ctx))
        location (:location (:payload (:user ctx)))
        payment (:payment (:payload (:user ctx)))
        comment (:comment (:payload (:user ctx)))
        card? (= payment u/card-payment)]
    (merge
      {:run            [(merge {:function ord/create-order!
                                :args     [basket-id location comment (:value payment)]}
                               (if card?
                                 {:next-event :c/send-invoice}
                                 {:next-event :c/active-order}))
                        (when (not card?)
                          {:function bsk/clear-basket!
                           :args     [basket-id]})]
       :delete-message {:chat-id    chat-id
                        :message-id message-id}})))


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


(defn active-order-text
  [order]
  (format (str "*Заказ №%s:*\n\n"
               (apply str (u/order-products-text (:products order)))
               "\n"
               u/money-emoji " %s сум (Наличными)\n\n"
               "Ваш заказ готовится, курьер приедет через 30 минут")
          (:id order)
          (u/fmt-values (:total_cost order))))


(defn product-price
  [product]
  {:label  (format "%d x %s" (:count product) (:name product))
   :amount (* (:price product) (:count product) 100)})


(defn order-prices
  [order]
  (map product-price (:products order)))


(defn active-order
  [ctx order]
  (if (int? order)
    {:run {:function   o/order-by-id!
           :args       [order]
           :next-event :c/active-order}}
    (let [update (:update ctx)
          chat-id (u/chat-id update)]
      {:send-text {:chat-id chat-id
                   :text    (active-order-text order)
                   :options {:parse_mode "markdown"}}})))


(defn invoice-description
  [order]
  (format (str (apply str (u/order-products-text (:products order))))))


(def invoice-reply-markup
  {:inline_keyboard [[{:text "Оплатить" :pay true}]]})


(defn send-invoice
  [ctx order]
  (if (int? order)
    {:run {:function   o/order-by-id!
           :args       [order]
           :next-event :c/send-invoice}}
    (let [update (:update ctx)
          chat-id (u/chat-id update)
          message-id (:message_id (:message (:callback_query update)))]
      {:send-invoice   {:chat-id     chat-id
                        :title       (str "Оплатить заказ №" (:id order))
                        :description (invoice-description order)
                        :payload     {:order_id (:id order)}
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
    {:dispatch       {:args [:c/active-order]}
     :delete-message {:chat-id    chat-id
                      :message-id message-id}}))


(d/register-event-handler!
  :c/to-order
  to-order-handler)


(d/register-event-handler!
  :c/order-confirmation-state
  order-confirmation-state)


(d/register-event-handler!
  :c/send-order-detail
  order-detail-handler)


(d/register-event-handler!
  :c/create-order
  create-order-handler)


(d/register-event-handler!
  :c/change-comment
  change-comment-handler)


(d/register-event-handler!
  :c/switch-payment-type
  switch-payment-type-handler)


(d/register-event-handler!
  :c/update-order-confirmation
  update-order-confirmation-handler)


(d/register-event-handler!
  :c/active-order
  active-order)


(d/register-event-handler!
  :c/send-invoice
  send-invoice)


(d/register-event-handler!
  :c/cancel-invoice
  cancel-invoice-handler)
