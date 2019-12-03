(ns yamfood.telegram.handlers.text
  (:require [morse.api :as t]
            [yamfood.core.products.core :as products]
            [yamfood.telegram.handlers.utils :as u]
            [yamfood.telegram.dispatcher :as d]
            [clojure.data.json :as json]))


(defn get-product-caption
  [product]
  (format (str u/food-emoji " *%s* \n\n"
               u/money-emoji "%s сум  " u/energy-emoji "%s кКал")
          (:name product)
          (u/fmt-values (:price product))
          (u/fmt-values (:energy product))))



(defn get-product-detail-options
  [product]
  {:caption      (get-product-caption product)
   :parse_mode   "markdown"
   :reply_markup (json/write-str (u/product-detail-markup product))})


(defn handle-text
  [ctx message]
  {:core {:function    #(products/get-product-by-name!
                          (:basket_id (:user ctx))
                          (:text message))
          :on-complete #(d/dispatch! ctx [:product-done message %])}})


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


(d/register-event-handler!
  :product-done
  react-to-text)


(d/register-event-handler!
  :text
  handle-text)


