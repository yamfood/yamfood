(ns yamfood.telegram.handlers.rider.text
  (:require
    [yamfood.core.orders.core :as o]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn rider-start-handler
  [ctx]
  (let [update (:update ctx)
        message (:message update)
        chat-id (:id (:from message))]
    {:send-text {:chat-id chat-id
                 :text    "Hello, Rider!"}}))


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


(defn rider-text-handler
  [ctx]
  (let [update (:update ctx)
        message (:message update)
        text (:text message)
        chat-id (:id (:from message))
        order-id (u/parse-int text)]
    (if order-id
      (let [order (o/order-by-id! order-id {:products? false})]
        (if order
          {:send-location {:chat-id   chat-id
                           :longitude (:longitude (:location order))
                           :latitude  (:latitude (:location order))}
           :send-text     {:chat-id chat-id
                           :text    (order-detail-text order)
                           :options {:parse_mode   "markdown"
                                     :reply_markup (order-detail-markup order)}}}

          {:send-text {:chat-id chat-id
                       :text    "Такого заказа не существует"}}))

      {:send-text {:chat-id chat-id
                   :text    "Отправьте номер заказа"}})))


(d/register-event-handler!
  :r/start
  rider-start-handler)


(d/register-event-handler!
  :r/text
  rider-text-handler)



