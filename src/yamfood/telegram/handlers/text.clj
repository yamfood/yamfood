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
  ([ctx]
   (let [update (:update ctx)
         message (:message update)]
     {:core {:function    #(products/product-detail-state-by-name!
                             (:basket_id (:user ctx))
                             (:text message))
             :on-complete #(d/dispatch! ctx [:text %])}}))
  ([ctx product-detail-state]
   (let [update (:update ctx)
         message (:message update)
         chat (:chat message)
         chat-id (:id chat)]
     (if product-detail-state
       {:send-photo
        {:chat-id chat-id
         :options (product-detail-options product-detail-state)
         :photo   (:photo product-detail-state)}}

       {:send-text
        {:chat-id chat-id
         :text    "Если у вас возникли какие-то вопросы обратитесь к @kensay."}}))))


(d/register-event-handler!
  :text
  product-detail-handler)
