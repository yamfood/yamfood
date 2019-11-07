(ns yamfood.telegram.handlers.text
  (:require [morse.api :as t]
            [yamfood.core.products.core :as products]
            [clojure.data.json :as json]
            [yamfood.telegram.dispatcher :as d]))


(defn get-product-caption
  [product]
  (format "*%s* \n\n *Цена:* %,d сум."
          (:name product)
          (:price product)))


(defn get-product-markup
  []
  ; Because morse send photo by multipart and not convert it.
  (json/write-str
    {:inline_keyboard
     [[{:text "Хочу" :callback_data "want"}]
      [{:text "Корзина" :callback_data "basket"}]
      [{:text                             "Еще!"
        :switch_inline_query_current_chat ""}]]}))


(defn get-product-detail-options
  [product]
  {:caption      (get-product-caption product)
   :parse_mode   "markdown"
   :reply_markup (get-product-markup)})


(defn handle-text
  [ctx message]
  {:core {:function    #(products/get-product-by-name! (:text message))
          :on-complete #(d/dispatch ctx [:product-done message %])}})


(defn handle-want
  [query]
  {:edit-reply-markup {:chat_id      (:id (:from query))
                       :message_id   (:message_id (:message query))
                       :reply_markup {:inline_keyboard
                                      [[{:text "-" :callback_data "-"}
                                        {:text "1" :callback_data "1"}
                                        {:text "+" :callback_data "+"}]
                                       [{:text "Корзина (1)" :callback_data "basket"}]
                                       [{:text                             "Еще!"
                                         :switch_inline_query_current_chat ""}]]}}
   :answer-callback   {:callback_query_id (:id query)
                       :text              "Добавлено в корзину"}})


(defn handle-callback
  [_ {:keys [callback_query]}]
  (when (= (:data callback_query) "want")
    (handle-want callback_query)))


(defn react-to-text
  [_ message product]
  (let [chat (:chat message)
        chat-id (:id chat)]
    (if product
      {:send-photo
       {:chat-id chat-id
        :options (get-product-detail-options product)
        :photo   (:photo product)}}

      {:send-text
       {:chat-id chat-id
        :text    "Если у вас возникли какие-то вопросы обратитесь к @kensay."}})))
