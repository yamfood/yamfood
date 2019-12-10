(ns yamfood.telegram.handlers.utils-test
  (:require
    [clojure.test :refer :all]
    [yamfood.telegram.handlers.utils :as utils]))


(def message-upd
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


(def callback-upd
  {:update_id      435323133,
   :callback_query {:id            "340271653755566136",
                    :from          {:id            79225668,
                                    :is_bot        false,
                                    :first_name    "Рустам",
                                    :last_name     "Бабаджанов",
                                    :username      "kensay",
                                    :language_code "ru"},
                    :message       {:message_id   10183,
                                    :from         {:id         488312680,
                                                   :is_bot     true,
                                                   :first_name "Kensay",
                                                   :username   "kensaybot"},
                                    :chat         {:id         79225668,
                                                   :first_name "Рустам",
                                                   :last_name  "Бабаджанов",
                                                   :username   "kensay",
                                                   :type       "private"},
                                    :date         1575998859,
                                    :text         "Заказ №1334:\n\n\n💰 53 200 сум (Не оплачено)\n\nОжидает подтверждения оператором",
                                    :entities     [{:offset 0, :length 12, :type "bold"}],
                                    :reply_markup {:inline_keyboard [[{:text          "Оплатить картой",
                                                                       :callback_data "invoice/"}]]}},
                    :chat_instance "4402156230761928760",
                    :data          "invoice/123"}})


(deftest utils-test
  (testing "Test chat-id function"
    (is (= (utils/chat-id message-upd)
           (:id (:from (:message message-upd)))))
    (is (= (utils/chat-id callback-upd)
           (:id (:from (:callback_query callback-upd))))))
  (testing "Test callback-params function"
    (is (= (utils/callback-params (:data (:callback_query callback-upd)))
           ["123"]))))
