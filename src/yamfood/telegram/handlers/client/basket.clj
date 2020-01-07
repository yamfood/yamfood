(ns yamfood.telegram.handlers.client.basket
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.baskets.core :as baskets]
    [yamfood.telegram.handlers.utils :as u]))


(defn basket-inc-handler
  [ctx]
  (let [update (:update ctx)
        callback-query (:callback_query update)
        callback-data (:data callback-query)
        basket-id (:basket_id (:user ctx))
        product-id (Integer.
                     (first (u/callback-params callback-data)))]
    {:run             [{:function baskets/increment-product-in-basket!
                        :args     [basket-id product-id]}
                       {:function   baskets/basket-state!
                        :args       [basket-id]
                        :next-event :c/update-basket-markup}]
     :answer-callback {:callback_query_id (:id callback-query)
                       :text              " "}}))


(defn basket-dec-handler
  [ctx]
  (let [update (:update ctx)
        callback-query (:callback_query update)
        callback-data (:data callback-query)
        basket-id (:basket_id (:user ctx))
        product-id (Integer.
                     (first (u/callback-params callback-data)))]
    {:run             [{:function baskets/decrement-product-in-basket!
                        :args     [basket-id product-id]}
                       {:function   baskets/basket-state!
                        :args       [basket-id]
                        :next-event :c/update-basket-markup}]
     :answer-callback {:callback_query_id (:id callback-query)
                       :text              " "}}))


(defn basket-handler
  [ctx]
  {:run {:function   baskets/basket-state!
         :args       [(:basket_id (:user ctx))]
         :next-event :c/send-basket}})


(defn basket-product-markup
  [val product]
  (apply conj val [[{:callback_data "nothing"
                     :text          (format (str u/food-emoji " %d x %s")
                                            (:count product)
                                            (:name product))}]

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
  [ctx basket-state]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))]
    {:send-text      {:chat-id chat-id
                      :text    "Ваша корзина:"
                      :options {:reply_markup (basket-detail-markup basket-state)}}
     :delete-message {:chat-id    chat-id
                      :message-id message-id}}))


(defn update-basket-markup
  [ctx basket-state]
  (let [update (:update ctx)
        query (:callback_query update)]
    {:edit-reply-markup {:chat_id      (:id (:from query))
                         :message_id   (:message_id (:message query))
                         :reply_markup (basket-detail-markup basket-state)}}))


(d/register-event-handler!
  :c/basket
  basket-handler)


(d/register-event-handler!
  :c/inc-basket-product
  basket-inc-handler)


(d/register-event-handler!
  :c/dec-basket-product
  basket-dec-handler)


(d/register-event-handler!
  :c/send-basket
  send-basket)


(d/register-event-handler!
  :c/update-basket-markup
  update-basket-markup)