(ns yamfood.telegram.handlers.client.feedback
  (:require
    [yamfood.core.orders.core :as o]
    [yamfood.core.params.core :as p]
    [yamfood.core.clients.core :as c]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.helpers.notifier :as n]
    [yamfood.telegram.translation.core :refer [translate]]))


(defn feedback-handler
  [ctx]
  (let [query (:callback_query (:update ctx))
        params (u/callback-params (:data query))
        order-id (u/parse-int (first params))
        rate (second params)]
    {:run      {:function o/update!
                :args     [order-id {:rate rate}]}
     :dispatch {:args [:c/request-text-feedback order-id]}}))


(defn text-feedback-markup
  [lang]
  {:resize_keyboard true
   :keyboard        [[{:text (translate lang :feedback-ok)}]
                     [{:text (translate lang :feedback-long-delivery)}]
                     [{:text (translate lang :feedback-cold-food)}]
                     [{:text (translate lang :feedback-incomplete-order)}]
                     [{:text (translate lang :feedback-no-cutlery)}]
                     [{:text (translate lang :feedback-bad-courier)}]]})


(defn request-text-feedback
  [ctx order-id]
  (let [update (:update ctx)
        query (:callback_query update)
        client (:client ctx)
        lang (:lang ctx)
        payload (:payload client)
        chat-id (u/chat-id update)]
    {:delete-message {:chat-id    chat-id
                      :message-id (:message_id (:message query))}
     :run            {:function c/update-payload!
                      :args     [(:id client) (-> payload
                                                  (assoc :step u/feedback-step)
                                                  (assoc :last-order-id order-id))]}
     :send-text      {:chat-id chat-id
                      :text    (translate lang :request-text-feedback)
                      :options {:reply_markup (text-feedback-markup lang)}}}))


(defn text-feedback-handler
  [ctx]
  (let [update (:update ctx)
        chat-id (u/chat-id update)
        params (p/params!)
        forward-feedback? (seq (:notifier-bot-token params))
        lang (:lang ctx)
        client (:client ctx)
        payload (:payload client)
        last-order-id (:last-order-id payload)
        order (o/order-by-id! last-order-id)
        rate (str (:rate order) " " (:text (:message update)))]
    {:run       (into [{:function o/update!
                        :args     [(:id order) {:rate rate}]}]
                      (if forward-feedback?
                        [{:function n/forward-feedback!
                          :args     [(assoc order :rate rate)]}]
                        nil))
     :send-text {:chat-id chat-id
                 :text    (translate lang :accepted)
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
