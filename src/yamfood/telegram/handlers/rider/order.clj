(ns yamfood.telegram.handlers.rider.order
  (:require
    [yamfood.core.orders.core :as o]
    [yamfood.core.riders.core :as r]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.handlers.rider.core :as c]))


(defn- order-detail-text
  [order]
  (format
    (str "*---------- Заказ №%s ----------*\n\n"
         u/client-emoji " %s\n"
         u/phone-emoji " +%s\n"
         u/money-emoji " %s сум\n\n"
         u/comment-emoji " %s\n")
    (:id order)
    (:name order)
    (:phone order)
    (u/fmt-values (:total_cost order))
    (:comment order)))


(defn- order-detail-markup
  [order]
  {:inline_keyboard
   [[{:text          (str u/food-emoji " Продукты")
      :callback_data (str "order-products/" (:id order))}]
    [{:text          (str u/finish-emoji " Завершить")
      :callback_data (str "finish-order/" (:id order))}
     {:text          (str u/cancel-emoji " Отменить")
      :callback_data (str "cancel-order/" (:id order))}]]})


(defn send-order-detail
  ([ctx]
   (let [update (:update ctx)
         query (:callback_query update)
         data (:data query)
         order-id (u/parse-int (first (u/callback-params data)))]
     {:run {:function   o/order-by-id!
            :args       [order-id]
            :next-event :r/order-detail}}))
  ([ctx order]
   (let [chat-id (u/chat-id (:update ctx))]
     {:send-location {:chat-id   chat-id
                      :longitude (:longitude (:location order))
                      :latitude  (:latitude (:location order))}
      :send-text     {:chat-id chat-id
                      :text    (order-detail-text order)
                      :options {:parse_mode   "markdown"
                                :reply_markup (order-detail-markup order)}}})))


(defn- assign-order
  [ctx order-id]
  (let [chat-id (u/chat-id (:update ctx))
        order (o/order-by-id! order-id {:products? false})]
    (if order
      (merge {:run {:function r/assign-rider-to-order!
                    :args     [order-id (:id (:rider ctx))]}}
             (send-order-detail ctx order))

      {:send-text {:chat-id chat-id
                   :text    "Такого заказа не существует"}})))


(defn rider-assign-order-handler
  [ctx]
  (let [update (:update ctx)
        message (:message update)
        text (:text message)
        chat-id (:id (:from message))
        rider (:rider ctx)
        order-id (u/parse-int text)]
    (if order-id
      (if (= (:active-order rider) nil)
        (assign-order ctx order-id)
        {:send-text {:chat-id chat-id
                     :text    "У вас уже есть активный заказ"}})

      {:send-text {:chat-id chat-id
                   :text    "Отправьте номер заказа"}})))


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
                        :text              (apply str (u/order-products-text products))
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
         query (:callback_query update)
         chat-id (u/chat-id update)
         message-id (:message_id (:message query))
         rider (:rider ctx)]
     {:run             {:function r/finish-order!
                        :args     [(:id order) (:id rider)]}
      :answer-callback {:callback_query_id (:id query)
                        :text              "Заказ успешно завершен!"
                        :show_alert        true}
      :dispatch        {:args        [:r/menu]
                        :rebuild-ctx {:function c/build-ctx!
                                      :update   update}}
      :delete-message  {:chat-id    chat-id
                        :message-id message-id}})))


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
         query (:callback_query update)
         chat-id (u/chat-id update)
         message-id (:message_id (:message query))
         rider (:rider ctx)]
     {:run             {:function r/cancel-order!
                        :args     [(:id order) (:id rider)]}
      :answer-callback {:callback_query_id (:id query)
                        :text              "Заказ отменен!"
                        :show_alert        true}
      :dispatch        {:args        [:r/menu]
                        :rebuild-ctx {:function c/build-ctx!
                                      :update   update}}
      :delete-message  {:chat-id    chat-id
                        :message-id message-id}})))


(d/register-event-handler!
  :r/text
  rider-assign-order-handler)


(d/register-event-handler!
  :r/order-products
  order-products)


(d/register-event-handler!
  :r/finish-order
  finish-order!)


(d/register-event-handler!
  :r/cancel-order
  cancel-order!)


(d/register-event-handler!
  :r/order-detail
  send-order-detail)
