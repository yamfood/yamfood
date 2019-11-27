(ns yamfood.telegram.handlers.bucket
  (:require [yamfood.telegram.handlers.utils :as u]
            [yamfood.core.users.bucket :as b]
            [yamfood.telegram.dispatcher :as d]))


(defn handle-want
  [ctx update]
  (let [query (:callback_query update)
        user (:user ctx)
        callback-data (:data query)
        callback-params (u/get-callback-params callback-data)
        product-id (Integer. (first callback-params))]
    {:core            {:function    #(b/add-product-to-bucket! (:bucket_id user) product-id)
                       :on-complete #(d/dispatch ctx [:update-markup update %])}
     :answer-callback {:callback_query_id (:id query)
                       :text              "Добавлено в корзину"}}))

(defn handle-inc
  [ctx update]
  (let [callback-query (:callback_query update)
        callback-data (:data callback-query)
        bucket-id (:bucket_id (:user ctx))
        product-id (Integer.
                     (first (u/get-callback-params callback-data)))]
    {:core            {:function    #(b/increment-product-in-bucket!
                                       bucket-id
                                       product-id)
                       :on-complete #(d/dispatch ctx [:update-markup update %])}
     :answer-callback {:callback_query_id (:id callback-query)}}))

(defn handle-dec
  [ctx update]
  (let [callback-query (:callback_query update)
        callback-data (:data callback-query)
        bucket-id (:bucket_id (:user ctx))
        product-id (Integer.
                     (first (u/get-callback-params callback-data)))]
    {:core            {:function    #(b/decrement-product-in-bucket!
                                       bucket-id
                                       product-id)
                       :on-complete #(d/dispatch ctx [:update-markup update %])}
     :answer-callback {:callback_query_id (:id callback-query)}}))

(defn handle-bucket-inc
  [ctx update]
  (let [callback-query (:callback_query update)
        callback-data (:data callback-query)
        bucket-id (:bucket_id (:user ctx))
        product-id (Integer.
                     (first (u/get-callback-params callback-data)))]
    {:core            [{:function #(b/increment-product-in-bucket!
                                     bucket-id
                                     product-id)}
                       {:function    #(b/get-bucket-state! bucket-id)
                        :on-complete #(d/dispatch ctx [:update-bucket-markup update %])}]
     :answer-callback {:callback_query_id (:id callback-query)}}))


(defn handle-bucket-dec
  [ctx update]
  (let [callback-query (:callback_query update)
        callback-data (:data callback-query)
        bucket-id (:bucket_id (:user ctx))
        product-id (Integer.
                     (first (u/get-callback-params callback-data)))]
    {:core            [{:function #(b/decrement-product-in-bucket!
                                     bucket-id
                                     product-id)}
                       {:function    #(b/get-bucket-state! bucket-id)
                        :on-complete #(d/dispatch ctx [:update-bucket-markup update %])}]
     :answer-callback {:callback_query_id (:id callback-query)}}))

(defn update-markup
  [_ update product]
  (let [query (:callback_query update)]
    {:edit-reply-markup {:chat_id      (:id (:from query))
                         :message_id   (:message_id (:message query))
                         :reply_markup (u/product-detail-markup product)}}))


(defn handle-basket
  [ctx update]
  {:core {:function    #(b/get-bucket-state! (:bucket_id (:user ctx)))
          :on-complete #(d/dispatch ctx [:send-bucket update %])}})


(defn bucket-product-markup
  [val product]
  (apply conj val [[{:text (format "\uD83E\uDD57 %s" (:name product)) :callback_data "nothing"}]
                   (u/bucket-product-controls
                     "bucket"
                     (:id product)
                     (format "%,dсум. x %d" (:price product) (:count product)))]))

(defn bucket-detail-products-markup
  [bucket-state]
  (cond
    (empty? (:products bucket-state)) [[{:text "К сожалению, ваша корзина пока пуста :("
                                         :callback_data "nothing"}]]
    :else (reduce bucket-product-markup [] (:products bucket-state))))


(defn bucket-detail-markup
  [bucket-state]
  (let [total_cost (:total_cost bucket-state)
        total_energy (:total_energy bucket-state)]
    {:inline_keyboard
     (conj (bucket-detail-products-markup bucket-state)
           [{:text "Еще!" :switch_inline_query_current_chat ""}]
           [{:text (format "\uD83D\uDCB0 %,d сум. \uD83D\uDD0B %,d кКал." total_cost total_energy) :callback_data "nothing"}]
           [{:text "✅ Далее" :callback_data "nothing"}])}))


(defn send-bucket
  [_ update bucket-state]
  (let [query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))]
    {:delete-message {:chat-id chat-id
                      :message-id message-id}
     :send-text {:chat-id chat-id
                 :text    "Ваша корзина:"
                 :options {:reply_markup (bucket-detail-markup bucket-state)}}}))



(defn update-bucket-markup
  [_ update bucket-state]
  (let [query (:callback_query update)]
    {:edit-reply-markup {:chat_id      (:id (:from query))
                         :message_id   (:message_id (:message query))
                         :reply_markup (bucket-detail-markup bucket-state)}}))
