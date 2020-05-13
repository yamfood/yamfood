(ns yamfood.telegram.handlers.client.feedback
  (:require
    [yamfood.core.orders.core :as o]
    [yamfood.core.clients.core :as c]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn feedback-handler
  [ctx]
  (let [query (:callback_query (:update ctx))
        params (u/callback-params (:data query))
        order-id (u/parse-int (first params))
        rate (second params)]
    {:run      {:function o/update!
                :args     [order-id {:rate rate}]}
     :dispatch {:args [:c/request-text-feedback order-id]}}))


(def text-feedback-markup
  {:resize_keyboard true
   :keyboard        [[{:text "Не хочу"}]]})


(defn request-text-feedback
  [ctx order-id]
  (let [update (:update ctx)
        query (:callback_query update)
        client (:client ctx)
        payload (:payload client)
        chat-id (u/chat-id update)]
    {:delete-message {:chat-id    chat-id
                      :message-id (:message_id (:message query))}
     :run            {:function c/update-payload!
                      :args     [(:id client) (-> payload
                                                  (assoc :step u/feedback-step)
                                                  (assoc :last-order-id order-id))]}
     :send-text      {:chat-id chat-id
                      :text    "Оставьте отзыв к заказу!"
                      :options {:reply_markup text-feedback-markup}}}))


(defn text-feedback-handler
  [ctx]
  (let [update (:update ctx)
        chat-id (u/chat-id update)
        client (:client ctx)
        payload (:payload client)
        last-order-id (:last-order-id payload)
        order (o/order-by-id! last-order-id)]
    {:run       {:function o/update!
                 :args     [(:id order)
                            {:rate (str (:rate order) " " (:text (:message update)))}]}
     :send-text {:chat-id chat-id
                 :text    "Принято!"
                 :options {:reply_markup {:remove_keyboard true}}}
     :dispatch  {:args [:c/menu]}}))


(d/register-event-handler!
  :c/feedback
  feedback-handler)


(d/register-event-handler!
  :c/request-text-feedback
  request-text-feedback)


(d/register-event-handler!
  :c/text-feedback
  text-feedback-handler)
