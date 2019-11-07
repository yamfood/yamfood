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


(defn get-product-detail-options
  [product]
  {:caption      (get-product-caption product)
   :parse_mode   "markdown"
   :reply_markup (json/write-str                            ; Because morse send photo by multipart and not convert it.
                   {:inline_keyboard
                    [[{:text                             "Еще!"
                       :switch_inline_query_current_chat ""}]]})})


(defn handle-text
  [ctx message]
  {:core {:function    #(products/get-product-by-name! (:text message))
          :on-complete #(d/dispatch ctx [:product-done message %])}})


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
