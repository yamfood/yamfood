(ns yamfood.telegram.handlers.basket
  (:require [yamfood.telegram.handlers.utils :as u]
            [yamfood.core.baskets.core :as b]
            [yamfood.telegram.dispatcher :as d]))


(defn handle-want
  [ctx update]
  (let [query (:callback_query update)
        user (:user ctx)
        callback-data (:data query)
        callback-params (u/get-callback-params callback-data)
        product-id (Integer. (first callback-params))]
    {:core            {:function    #(b/add-product-to-basket! (:basket_id user) product-id)
                       :on-complete #(d/dispatch ctx [:update-markup update %])}
     :answer-callback {:callback_query_id (:id query)
                       :text              "Добавлено в корзину"}}))

(defn handle-inc
  [ctx update]
  (let [callback-query (:callback_query update)
        callback-data (:data callback-query)
        basket-id (:basket_id (:user ctx))
        product-id (Integer.
                     (first (u/get-callback-params callback-data)))]
    {:core            {:function    #(b/increment-product-in-basket!
                                       basket-id
                                       product-id)
                       :on-complete #(d/dispatch ctx [:update-markup update %])}
     :answer-callback {:callback_query_id (:id callback-query)}}))

(defn handle-dec
  [ctx update]
  (let [callback-query (:callback_query update)
        callback-data (:data callback-query)
        basket-id (:basket_id (:user ctx))
        product-id (Integer.
                     (first (u/get-callback-params callback-data)))]
    {:core            {:function    #(b/decrement-product-in-basket!
                                       basket-id
                                       product-id)
                       :on-complete #(d/dispatch ctx [:update-markup update %])}
     :answer-callback {:callback_query_id (:id callback-query)}}))

(defn handle-basket-inc
  [ctx update]
  (let [callback-query (:callback_query update)
        callback-data (:data callback-query)
        basket-id (:basket_id (:user ctx))
        product-id (Integer.
                     (first (u/get-callback-params callback-data)))]
    {:core            [{:function #(b/increment-product-in-basket!
                                     basket-id
                                     product-id)}
                       {:function    #(b/get-basket-state! basket-id)
                        :on-complete #(d/dispatch ctx [:update-basket-markup update %])}]
     :answer-callback {:callback_query_id (:id callback-query)}}))


(defn handle-basket-dec
  [ctx update]
  (let [callback-query (:callback_query update)
        callback-data (:data callback-query)
        basket-id (:basket_id (:user ctx))
        product-id (Integer.
                     (first (u/get-callback-params callback-data)))]
    {:core            [{:function #(b/decrement-product-in-basket!
                                     basket-id
                                     product-id)}
                       {:function    #(b/get-basket-state! basket-id)
                        :on-complete #(d/dispatch ctx [:update-basket-markup update %])}]
     :answer-callback {:callback_query_id (:id callback-query)}}))

(defn update-markup
  [_ update product]
  (let [query (:callback_query update)]
    {:edit-reply-markup {:chat_id      (:id (:from query))
                         :message_id   (:message_id (:message query))
                         :reply_markup (u/product-detail-markup product)}}))


(defn handle-basket
  [ctx update]
  {:core {:function    #(b/get-basket-state! (:basket_id (:user ctx)))
          :on-complete #(d/dispatch ctx [:send-basket update %])}})


(defn basket-product-markup
  [val product]
  (apply conj val [[{:text (format (str u/food-emoji " %s x %d") (:name product) (:count product)) :callback_data "nothing"}]
                   (u/basket-product-controls
                     "basket"
                     (:id product)
                     (format "%s сум"
                             (u/fmt-values (* (:price product) (:count product)))))]))


(defn basket-detail-products-markup
  [basket-state]
  (cond
    (empty? (:products basket-state)) [[{:text          "К сожалению, ваша корзина пока пуста :("
                                         :callback_data "nothing"}]]
    :else (reduce basket-product-markup [] (:products basket-state))))


(defn basket-detail-markup
  [basket-state]
  (let [total_cost (:total_cost basket-state)
        total_energy (:total_energy basket-state)]
    {:inline_keyboard
     (conj (basket-detail-products-markup basket-state)
           [{:text "Еще!" :switch_inline_query_current_chat ""}]
           [{:text          (format (str u/money-emoji " %s сум "
                                         u/energy-emoji " %s кКал")
                                    (u/fmt-values total_cost)
                                    (u/fmt-values total_energy))
             :callback_data "nothing"}]
           [{:text "✅ Далее" :callback_data "to-order"}])}))


(defn send-basket
  [_ update basket-state]
  (let [query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))]
    {:send-text      {:chat-id chat-id
                      :text    "Ваша корзина:"
                      :options {:reply_markup (basket-detail-markup basket-state)}}
     :delete-message {:chat-id    chat-id
                      :message-id message-id}}))



(defn update-basket-markup
  [_ update basket-state]
  (let [query (:callback_query update)]
    {:edit-reply-markup {:chat_id      (:id (:from query))
                         :message_id   (:message_id (:message query))
                         :reply_markup (basket-detail-markup basket-state)}}))


(d/register-event-handler!
  :update-markup
  update-markup)


(d/register-event-handler!
  :send-basket
  send-basket)


(d/register-event-handler!
  :update-basket-markup
  update-basket-markup)

