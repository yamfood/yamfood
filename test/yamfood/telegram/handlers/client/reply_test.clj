(ns yamfood.telegram.handlers.client.reply-test
  (:require
    [clojure.test :refer :all]
    [yamfood.core.users.core :as users]
    [yamfood.telegram.handlers.client.reply :as reply]))


(def comment-upd
  {:update_id 435323158
   :message   {:message_id       10197
               :from             {:id            79225668
                                  :is_bot        false
                                  :first_name    "Рустам"
                                  :last_name     "Бабаджанов"
                                  :username      "kensay"
                                  :language_code "ru"}
               :chat             {:id         79225668
                                  :first_name "Рустам"
                                  :last_name  "Бабаджанов"
                                  :username   "kensay"
                                  :type       "private"}
               :date             1576144148
               :reply_to_message {:message_id 10196
                                  :from       {:id         488312680
                                               :is_bot     true
                                               :first_name "Kensay"
                                               :username   "kensaybot"}
                                  :chat       {:id         79225668
                                               :first_name "Рустам"
                                               :last_name  "Бабаджанов"
                                               :username   "kensay"
                                               :type       "private"}
                                  :date       1576144142
                                  :text       "Напишите свой комментарий к заказу"}
               :text             "Тест"}})


(def unknown-upd
  {:update_id 435323158
   :message   {:message_id       10197
               :from             {:id            79225668
                                  :is_bot        false
                                  :first_name    "Рустам"
                                  :last_name     "Бабаджанов"
                                  :username      "kensay"
                                  :language_code "ru"}
               :chat             {:id         79225668
                                  :first_name "Рустам"
                                  :last_name  "Бабаджанов"
                                  :username   "kensay"
                                  :type       "private"}
               :date             1576144148
               :reply_to_message {:message_id 10196
                                  :from       {:id         488312680
                                               :is_bot     true
                                               :first_name "Kensay"
                                               :username   "kensaybot"}
                                  :chat       {:id         79225668
                                               :first_name "Рустам"
                                               :last_name  "Бабаджанов"
                                               :username   "kensay"
                                               :type       "private"}
                                  :date       1576144142
                                  :text       "тест"}
               :text             "Тест"}})


(def default-ctx
  {:token          "488312680:AAGsKHKufV9TQEAB8-g6INps-W82G_noRP8"
   :payments-token "371317599:TEST:79225668"
   :update         {}
   :user           {:id        10
                    :phone     998909296339
                    :tid       79225668
                    :location  {:longitude 34.74037 :latitude 32.020955}
                    :comment   "Тест"
                    :basket_id 4}})


(def comment-ctx
  (assoc default-ctx
    :update
    comment-upd))


(def unknown-ctx
  (assoc default-ctx
    :update
    unknown-upd))


(def comment-result
  {:run      {:function users/update-comment!
              :args     [10 "Тест"]}
   :dispatch {:args [:order-confirmation-state]}})


(def unknown-result
  {})


(deftest reply-handler-test
  (testing "Testing reply-handler"
    (is (= (reply/message-with-reply-handler comment-ctx)
           comment-result)))
  (testing "Testing reply-handler with unknown reply"
    (is (= (reply/message-with-reply-handler unknown-ctx)
           unknown-result))))