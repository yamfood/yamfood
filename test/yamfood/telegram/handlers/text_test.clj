(ns yamfood.telegram.handlers.text-test
  (:require
    [clojure.test :refer :all]
    [yamfood.telegram.handlers.text :as text]))


(def upd-with-random-text
  {:update_id 435323085,
   :message   {:message_id 10148,
               :from       {:id
                                           79225668,
                            :is_bot        false,
                            :first_name    "Рустам",
                            :last_name     "Бабаджанов",
                            :username      "kensay",
                            :language_code "ru"},
               :chat       {:id         79225668,
                            :first_name "Рустам",
                            :last_name  "Бабаджанов",
                            :username   "kensay",
                            :type       "private"},
               :date       1575898013,
               :text       "Psgb"}})


(def upd-with-product-name
  {:update_id 435323084,
   :message   {:message_id 10146,
               :from       {:id            79225668,
                            :is_bot        false,
                            :first_name    "Рустам",
                            :last_name     "Бабаджанов",
                            :username      "kensay",
                            :language_code "ru"},
               :chat       {:id         79225668,
                            :first_name "Рустам",
                            :last_name  "Бабаджанов",
                            :username   "kensay",
                            :type       "private"},
               :date       1575898003,
               :text       "Глазунья с болгарским перцем и паштетом"}})


(def ctx
  {:token          "488312680:AAGsKHKufV9TQEAB8-g6INps-W82G_noRP8",
   :payments-token "371317599:TEST:79225668",
   :user           {:id        10,
                    :phone     998909296339,
                    :tid       79225668,
                    :location  {:longitude 34.74037, :latitude 32.020955},
                    :comment   "Test",
                    :basket_id 4}})


(def product-not-in-basket
  {:id              1,
   :name            "Глазунья с болгарским перцем и паштетом",
   :price           15000,
   :photo           "https://i.ibb.co/T8TRNm3/image.png",
   :thumbnail       "https://i.ibb.co/T8TRNm3/image.png",
   :energy          360,
   :basket_cost     0,
   :count_in_basket 0})


(def product-in-basket
  {:id              1,
   :name            "Глазунья с болгарским перцем и паштетом",
   :price           15000,
   :photo           "https://i.ibb.co/T8TRNm3/image.png",
   :thumbnail       "https://i.ibb.co/T8TRNm3/image.png",
   :energy          360,
   :basket_cost     0,
   :count_in_basket 2})


(def result-with-product-not-in-basket
  {:send-photo {:chat-id 79225668,
                :options {:caption      "🥗 *Глазунья с болгарским перцем и паштетом* \n\n💰15 000 сум  🔋360 кКал",
                          :parse_mode   "markdown",
                          :reply_markup "{\"inline_keyboard\":[[{\"text\":\"\\u0425\\u043e\\u0447\\u0443\",\"callback_data\":\"want\\/1\"}],[{\"text\":\"\\u041a\\u043e\\u0440\\u0437\\u0438\\u043d\\u0430 (0 \\u0441\\u0443\\u043c)\",\"callback_data\":\"basket\"}],[{\"text\":\"\\u0415\\u0449\\u0435!\",\"switch_inline_query_current_chat\":\"\"}]]}"},
                :photo   "https://i.ibb.co/T8TRNm3/image.png"}})


(def result-with-product-in-basket
  {:send-photo {:chat-id 79225668,
                :options {:caption      "🥗 *Глазунья с болгарским перцем и паштетом* \n\n💰15 000 сум  🔋360 кКал",
                          :parse_mode   "markdown",
                          :reply_markup "{\"inline_keyboard\":[[{\"text\":\"-\",\"callback_data\":\"detail-\\/1\"},{\"text\":\"2\",\"callback_data\":\"nothing\"},{\"text\":\"+\",\"callback_data\":\"detail+\\/1\"}],[{\"text\":\"\\u041a\\u043e\\u0440\\u0437\\u0438\\u043d\\u0430 (0 \\u0441\\u0443\\u043c)\",\"callback_data\":\"basket\"}],[{\"text\":\"\\u0415\\u0449\\u0435!\",\"switch_inline_query_current_chat\":\"\"}]]}"},
                :photo   "https://i.ibb.co/T8TRNm3/image.png"}})


(def result-with-random-text
  {:send-text {:chat-id 79225668, :text "Если у вас возникли какие-то вопросы обратитесь к @kensay."}})


(deftest text-handler-test
  (testing "Testing text update with product which is not in basket yet"
    (is (= (text/product-detail-handler ctx
                                        upd-with-product-name
                                        product-not-in-basket)
           result-with-product-not-in-basket)))
  (testing "Testing text update with product in basket"
    (is (= (text/product-detail-handler ctx
                                        upd-with-product-name
                                        product-in-basket)
           result-with-product-in-basket)))
  (testing "Testing text update with random text"
    (is (= (text/product-detail-handler ctx
                                        upd-with-random-text
                                        nil)
           result-with-random-text))))

