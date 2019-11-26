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
    {:core {:function    #(b/increment-product-in-bucket!
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
    {:core {:function    #(b/decrement-product-in-bucket!
                            bucket-id
                            product-id)
            :on-complete #(d/dispatch ctx [:update-markup update %])}
     :answer-callback {:callback_query_id (:id callback-query)}}))

(defn update-markup
  [_ update product]
  (let [query (:callback_query update)]
    {:edit-reply-markup {:chat_id      (:id (:from query))
                         :message_id   (:message_id (:message query))
                         :reply_markup (u/product-detail-markup product)}}))


(handle-want {} {:update_id 435322471, :callback_query {:id "340271654695056577", :from {:id 79225668, :is_bot false, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :language_code "ru"}, :message {:message_id 9639, :from {:id 488312680, :is_bot true, :first_name "Kensay", :username "kensaybot"}, :chat {:id 79225668, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :type "private"}, :date 1574768353, :photo [{:file_id "AgADBAADKaoxG0IYJVF7zRhSNk9rn0GDoBsABAEAAwIAA20AA37BAgABFgQ", :file_size 8077, :width 300, :height 300}], :caption "Рисовая каша с ежевикой \n\n Цена: 13,800 сум.", :caption_entities [{:offset 0, :length 23, :type "bold"} {:offset 27, :length 5, :type "bold"}], :reply_markup {:inline_keyboard [[{:text "Хочу", :callback_data "want/2"}] [{:text "Корзина", :callback_data "basket"}] [{:text "Еще!", :switch_inline_query_current_chat ""}]]}}, :chat_instance "4402156230761928760", :data "want/2"}})