(ns yamfood.telegram.handlers.start-test
  (:require [clojure.test :refer :all]
            [yamfood.telegram.handlers.start :as start]))


(def upd
  {:update_id 435323081,
   :message   {:message_id 10144,
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
               :date       1575727101,
               :text       "/start",
               :entities   [{:offset 0,
                             :length 6,
                             :type   "bot_command"}]}})


(def existing-user-ctx
  {:token          "488312680:AAGsKHKufV9TQEAB8-g6INps-W82G_noRP8",
   :payments-token "371317599:TEST:79225668",
   :user           {:id        10,
                    :phone     998909296339,
                    :tid       79225668,
                    :location  {:longitude 34.74037,
                                :latitude  32.020955},
                    :comment   "Test",
                    :basket_id 4}})


(def new-user-ctx
  {:token          "488312680:AAGsKHKufV9TQEAB8-g6INps-W82G_noRP8",
   :payments-token "371317599:TEST:79225668",
   :user           nil})


(def existing-user-result
  {:send-text {:chat-id 79225668,
               :options {:reply_markup {:inline_keyboard [[{:text "Что поесть?", :switch_inline_query_current_chat ""}]
                                                          [{:text "Куда доставляете?", :callback_data "location-check"}]]}},
               :text    "Готовим и бесплатно доставляем за 30 минут"}})


(def new-user-result
  {:send-text {:chat-id 79225668,
               :options {:reply_markup {:resize_keyboard true,
                                        :keyboard        [[{:text "Отправить контакт", :request_contact true}]]}},
               :text    "Отправьте, пожалуйста, свой контакт"}})


(deftest start-handler-test
  (testing "Test start with existing user"
    (is (= (start/start-handler existing-user-ctx upd)
           existing-user-result)))
  (testing "Test start with new user"
    (is (= (start/start-handler new-user-ctx upd)
           new-user-result))))
