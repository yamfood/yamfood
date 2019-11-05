(ns yamfood.telegram.handlers.text
  (:require [morse.api :as t]
            [yamfood.core.products.core :as products]
            [clojure.data.json :as json]))



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
  (let [product (products/get-product-by-name! (:text message))
        chat (:chat message)
        chat-id (:id chat)]
    (if product
      (t/send-photo (:token ctx) chat-id
                    (get-product-detail-options product)
                    (:photo product))

      (t/send-text (:token ctx) chat-id
                   "Если у вас возникли какие-то вопросы обратитесь к @kensay."))))
