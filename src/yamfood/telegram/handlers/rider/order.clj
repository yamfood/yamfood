(ns yamfood.telegram.handlers.rider.order
  (:require
    [yamfood.core.orders.core :as o]
    [yamfood.core.riders.core :as r]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


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
    [{:text (str u/finish-emoji " Завершить") :callback_data "send-menu"}
     {:text (str u/cancel-emoji " Отменить") :callback_data "send-menu"}]]})



(defn- find-order
  [ctx chat-id order-id]
  (let [order (o/order-by-id! order-id {:products? false})]
    (if order
      {:run           {:function r/assign-rider-to-order!
                       :args     [order-id (:id (:rider ctx))]}
       :send-location {:chat-id   chat-id
                       :longitude (:longitude (:location order))
                       :latitude  (:latitude (:location order))}
       :send-text     {:chat-id chat-id
                       :text    (order-detail-text order)
                       :options {:parse_mode   "markdown"
                                 :reply_markup (order-detail-markup order)}}}

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
        (find-order ctx chat-id order-id)
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


(d/register-event-handler!
  :r/text
  rider-assign-order-handler)


(d/register-event-handler!
  :r/order-products
  order-products)
