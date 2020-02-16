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


(defn create-order-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))
        basket-id (:basket_id (:user ctx))
        location (:location (:user ctx))
        comment (:comment (:user ctx))]
    {:run            {:function ord/create-order-and-clear-basket!
                      :args     [basket-id location comment]}
     :dispatch       {:args        [:c/active-order]
                      :rebuild-ctx {:function c/build-ctx!
                                    :update   (:update ctx)}}
     :delete-message {:chat-id    chat-id
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


(defn active-order-text
  [order]
  (format (str "*Заказ №%s:*\n\n"
               (apply str (u/order-products-text (:products order)))
               "\n"
               u/money-emoji " %s сум (Наличными)\n\n"
               "Ваш заказ готовится, курьер приедет через 30 минут")
          (:id order)
          (u/fmt-values (:total_cost order))))


(defn active-order-reply-markup
  [order]
  {:inline_keyboard [[{:text "Оплатить картой" :callback_data (str "invoice/" (:id order))}]]})


(defn product-price
  [product]
  {:label  (format "%d x %s" (:count product) (:name product))
   :amount (* (:price product) 100)})


(defn order-prices
  [order]
  (map product-price (:products order)))


(defn active-order
  ([ctx]
   (let [user (:user ctx)
         order-id (:active_order_id user)]
     {:run {:function   o/order-by-id!
            :args       [order-id]
            :next-event :c/active-order}}))
  ([ctx order]
   (let [update (:update ctx)
         chat-id (u/chat-id update)]
     {:send-text {:chat-id chat-id
                  :text    (active-order-text order)
                  :options {:parse_mode   "markdown"
                            :reply_markup (active-order-reply-markup order)}}})))


(defn invoice-description
  [order]
  (format (str (apply str (u/order-products-text (:products order))))))


(def invoice-reply-markup
  {:inline_keyboard [[{:text "Оплатить" :pay true}]
                     [{:text "Отмена" :callback_data "cancel-invoice"}]]})


(defn send-invoice
  ([ctx]
   {:run {:function   ord/user-active-order!
          :args       [(:id (:user ctx))]
          :next-event :c/send-invoice}})
  ([ctx order]
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
  :c/active-order
  active-order)


(d/register-event-handler!
  :c/send-invoice
  send-invoice)


(d/register-event-handler!
  :c/cancel-invoice
  cancel-invoice-handler)
