(ns yamfood.telegram.handlers.rider.order
  (:require
    [yamfood.core.orders.core :as o]
    [yamfood.core.riders.core :as r]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.handlers.emojies :as e]
    [yamfood.telegram.handlers.rider.core :as c]))


(defn- order-detail-text
  [order]
  (format
    (str "*---------- Заказ №%s ----------*\n\n"
         e/client-emoji " %s\n"
         e/phone-emoji " +%s\n"
         e/money-emoji " %s\n\n"
         e/comment-emoji " %s\n\n"
         e/comment-emoji " %s\n")
    (:id order)
    (:name order)
    (:phone order)
    (if (= (:payment order) u/cash-payment)
      (str (u/fmt-values (+ (:total_cost order)
                            (:delivery_cost order)))
           " сум")
      "Оплачено картой")
    (or (:comment order) "Пусто...")
    (:notes order)))


(defn- order-detail-markup
  [order]
  {:inline_keyboard
   [[{:text          (str e/food-emoji " Продукты")
      :callback_data (str "order-products/" (:id order))}]
    [{:text          (str e/finish-emoji " Завершить")
      :callback_data (str "finish-order/" (:id order))}
     {:text          (str e/cancel-emoji " Отменить")
      :callback_data (str "cancel-order/" (:id order))}]]})


(defn assign-order
  [ctx]
  (let [update (:update ctx)
        chat-id (u/chat-id update)
        query (:callback_query update)
        order-id (u/parse-int (first (u/callback-params (:data query))))
        order (o/order-by-id! order-id {:products? false})]
    (if order
      {:run      {:function r/assign-rider-to-order!
                  :args     [order-id (:id (:rider ctx))]}
       :dispatch {:args [:r/update-preview-markup order]}}
      {:send-text {:chat-id chat-id
                   :text    "Такого заказа не существует"}})))


(defn order-preview-markup
  [order]
  (let [order-id (:id order)]
    {:inline_keyboard
     [[{:text "Принять?" :callback_data "nothing"}]
      [{:text "✅ Да" :callback_data (str "assign/" order-id)}
       {:text "❌ Нет" :callback_data "decline"}]]}))


(defn send-order-preview
  ([ctx]
   (let [update (:update ctx)
         message (:message update)
         text (:text message)
         chat-id (:id (:from message))
         order-id (u/parse-int text)]
     (if order-id
       {:run {:function   o/order-by-id!
              :args       [order-id]
              :next-event :r/text}}
       {:send-text {:chat-id chat-id
                    :text    "Отправьте номер заказа"}})))
  ([ctx order]
   (let [update (:update ctx)
         message (:message update)
         text (order-detail-text order)
         chat-id (:id (:from message))
         valid? (and order
                     (= (:status order) (:on-kitchen o/order-statuses)))]
     (if valid?
       {:send-location {:chat-id   chat-id
                        :longitude (:longitude (:location order))
                        :latitude  (:latitude (:location order))}
        :send-text     {:chat-id chat-id
                        :text    text
                        :options {:reply_markup (order-preview-markup order)
                                  :parse_mode   "markdown"}}}
       {:send-text {:chat-id chat-id
                    :text    "Такого заказа не существует"}}))))


(defn update-preview-markup
  [ctx order]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (u/chat-id update)
        message (:message query)]
    {:edit-reply-markup {:chat_id      chat-id
                         :message_id   (:message_id message)
                         :reply_markup (order-detail-markup order)}}))


(defn decline-order
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (u/chat-id update)
        message (:message query)]
    {:delete-message {:chat-id    chat-id
                      :message-id (:message_id message)}}))


(defn order-products
  ([ctx]
   (let [update (:update ctx)
         query (:callback_query update)
         callback-params (u/callback-params (:data query))
         order-id (u/parse-int (first callback-params))]
     {:run {:function   o/products-by-order-id!
            :args       [order-id]
            :next-event :r/order-products}}))
  ([ctx products]
   (let [update (:update ctx)
         query (:callback_query update)]
     {:answer-callback {:callback_query_id (:id query)
                        :text              (apply str (u/order-products-text :ru products))
                        :show_alert        true}})))


(defn finish-order!
  ([ctx]
   (let [update (:update ctx)
         query (:callback_query update)
         callback_params (u/callback-params (:data query))
         order-id (u/parse-int (first callback_params))]
     {:run {:function   o/order-by-id!
            :args       [order-id {:products? false :totals? false}]
            :next-event :r/finish-order}}))
  ([ctx order]
   (let [update (:update ctx)
         rider (:rider ctx)]
     {:run      {:function r/finish-order!
                 :args     [(:id order) (:id rider)]}
      :dispatch {:args        [:r/menu]
                 :rebuild-ctx {:function c/build-ctx!
                               :update   update}}})))


(defn cancel-order!
  ([ctx]
   (let [update (:update ctx)
         query (:callback_query update)
         callback_params (u/callback-params (:data query))
         order-id (u/parse-int (first callback_params))]
     {:run {:function   o/order-by-id!
            :args       [order-id {:products? false :totals? false}]
            :next-event :r/cancel-order}}))
  ([ctx order]
   (let [update (:update ctx)
         rider (:rider ctx)]
     {:run      {:function r/cancel-order!
                 :args     [(:id order) (:id rider)]}
      :dispatch {:args        [:r/menu]
                 :rebuild-ctx {:function c/build-ctx!
                               :update   update}}})))


(d/register-event-handler!
  :r/text
  send-order-preview)


(d/register-event-handler!
  :r/assign
  assign-order)


(d/register-event-handler!
  :r/decline
  decline-order)


(d/register-event-handler!
  :r/update-preview-markup
  update-preview-markup)


(d/register-event-handler!
  :r/order-products
  order-products)


(d/register-event-handler!
  :r/finish-order
  finish-order!)


(d/register-event-handler!
  :r/cancel-order
  cancel-order!)
