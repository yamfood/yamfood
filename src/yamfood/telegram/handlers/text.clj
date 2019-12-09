(ns yamfood.telegram.handlers.text
  (:require
    [clojure.data.json :as json]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.core.products.core :as products]))


(defn product-caption
  [product]
  (format (str u/food-emoji " *%s* \n\n"
               u/money-emoji "%s сум  " u/energy-emoji "%s кКал")
          (:name product)
          (u/fmt-values (:price product))
          (u/fmt-values (:energy product))))


(defn product-detail-options
  [product]
  {:caption      (product-caption product)
   :parse_mode   "markdown"
   :reply_markup (json/write-str (u/product-detail-markup product))})


(defn product-detail-handler
  ([ctx update]
   (let [message (:message update)]
     {:core {:function    #(products/product-by-name!
                             (:basket_id (:user ctx))
                             (:text message))
             :on-complete #(d/dispatch! ctx [:text update %])}}))
  ([_ update product]
   (let [message (:message update)
         chat (:chat message)
         chat-id (:id chat)]
     (if product
       {:send-photo
        {:chat-id chat-id
         :options (product-detail-options product)
         :photo   (:photo product)}}

       {:send-text
        {:chat-id chat-id
         :text    "Если у вас возникли какие-то вопросы обратитесь к @kensay."}}))))


(d/register-event-handler!
  :text
  product-detail-handler)
