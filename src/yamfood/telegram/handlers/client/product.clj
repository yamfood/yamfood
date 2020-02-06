(ns yamfood.telegram.handlers.client.product
  (:require
    [clojure.data.json :as json]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.baskets.core :as baskets]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.core.products.core :as products]))


(defn want-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        user (:user ctx)
        callback-data (:data query)
        callback-params (u/callback-params callback-data)
        product-id (Integer. (first callback-params))]
    {:run             {:function   baskets/add-product-to-basket!
                       :args       [(:basket_id user) product-id]
                       :next-event :c/update-markup}
     :answer-callback {:callback_query_id (:id query)
                       :text              "Добавлено в корзину"}}))


(defn detail-inc-handler
  [ctx]
  (let [update (:update ctx)
        callback-query (:callback_query update)
        callback-data (:data callback-query)
        basket-id (:basket_id (:user ctx))
        product-id (Integer. (first (u/callback-params callback-data)))]
    {:run             {:function   baskets/increment-product-in-basket!
                       :args       [basket-id product-id]
                       :next-event :c/update-markup}
     :answer-callback {:callback_query_id (:id callback-query)
                       :text              " "}}))


(defn detail-dec-handler
  [ctx]
  (let [update (:update ctx)
        callback-query (:callback_query update)
        callback-data (:data callback-query)
        basket-id (:basket_id (:user ctx))
        product-id (Integer. (first (u/callback-params callback-data)))]
    {:run             {:function   baskets/decrement-product-in-basket!
                       :args       [basket-id product-id]
                       :next-event :c/update-markup}
     :answer-callback {:callback_query_id (:id callback-query)
                       :text              " "}}))


(defn update-detail-markup
  [ctx product-state]
  (let [update (:update ctx)
        query (:callback_query update)]
    {:edit-reply-markup {:chat_id      (:id (:from query))
                         :message_id   (:message_id (:message query))
                         :reply_markup (u/product-detail-markup product-state)}}))


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
     {:run {:function   products/product-detail-state-by-name!
            :args       [(:basket_id (:user ctx)) (:text message)]
            :next-event :c/ttext}}))
  ([ctx product-detail-state]
   (let [update (:update ctx)
         message (:message update)
         chat (:chat message)
         chat-id (:id chat)]
     (if product-detail-state
       {:send-photo     {:chat-id chat-id
                         :options (product-detail-options product-detail-state)
                         :photo   (:photo product-detail-state)}
        :delete-message {:chat-id    chat-id
                         :message-id (:message_id message)}}

       {:dispatch {:args [:c/text]}}))))


(d/register-event-handler!
  :c/text
  product-detail-handler)


(d/register-event-handler!
  :c/detail-want
  want-handler)


(d/register-event-handler!
  :c/detail-inc
  detail-inc-handler)


(d/register-event-handler!
  :c/detail-dec
  detail-dec-handler)


(d/register-event-handler!
  :c/update-markup
  update-detail-markup)
