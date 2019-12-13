(ns yamfood.telegram.handlers.order-test
  (:require
    [clojure.test :refer :all]
    [yamfood.core.baskets.core :as basket]
    [yamfood.telegram.handlers.order :as order]))


(def default-ctx
  {:token          "488312680:AAGsKHKufV9TQEAB8-g6INps-W82G_noRP8",
   :payments-token "371317599:TEST:79225668",
   :update         {}
   :user           {:id        10,
                    :phone     998909296339,
                    :tid       79225668,
                    :location  {:longitude 34.74037, :latitude 32.020955},
                    :comment   "Test",
                    :basket_id 4}})


(def to-order-upd
  {:update_id      435323161,
   :callback_query {:id      "340271656533503070",
                    :from    {:id            79225668,
                              :is_bot        false,
                              :first_name    "Рустам",
                              :last_name     "Бабаджанов",
                              :username      "kensay",
                              :language_code "ru"},
                    :message {:message_id 10199,
                              :from       {:id         488312680,
                                           :is_bot     true,
                                           :first_name "Kensay",
                                           :username   "kensaybot"},
                              :chat       {:id         79225668,
                                           :first_name "Рустам",
                                           :last_name  "Бабаджанов",
                                           :username   "kensay",
                                           :type       "private"},
                              :date       1576241730,}
                    :data    "to-order"}})


(def to-order-ctx
  (assoc default-ctx
    :update
    to-order-upd))


(def to-order-ctx-without-location
  (let [user (:user to-order-ctx)]
    (assoc to-order-ctx
      :user
      (assoc user :location nil))))


(def to-order-result
  {:run            {:function   basket/pre-order-state!,
                    :args       [4],
                    :next-event :send-order-detail},
   :delete-message {:chat-id 79225668, :message-id 10199}})


(def to-order-without-location-result
  {:send-text      {:chat-id 79225668,
                    :text    "Куда доставить?",
                    :options {:resize_keyboard true,
                              :keyboard        [[{:text "Отправить текущее положение", :request_location true}]]}},
   :delete-message {:chat-id 79225668, :message-id 10199}})


(deftest to-order-test
  (testing "Test to-order"
    (is (= (order/to-order-handler to-order-ctx)
           to-order-result)))
  (testing "Test to-order with context without location"
    (is (= (order/to-order-handler to-order-ctx-without-location)
           to-order-without-location-result))))


(def request-location-upd
  {:update_id      435323162,
   :callback_query {:id      "340271655233464235",
                    :from    {:id            79225668,
                              :is_bot        false,
                              :first_name    "Рустам",
                              :last_name     "Бабаджанов",
                              :username      "kensay",
                              :language_code "ru"},
                    :message {:message_id 10201,
                              :from       {:id         488312680,
                                           :is_bot     true,
                                           :first_name "Kensay",
                                           :username   "kensaybot"},
                              :chat       {:id         79225668,
                                           :first_name "Рустам",
                                           :last_name  "Бабаджанов",
                                           :username   "kensay",
                                           :type       "private"},
                              :date       1576241740}
                    :data    "request-location"}})


(def request-location-ctx
  (assoc default-ctx
    :update
    request-location-upd))


(def request-location-result
  {:send-text      {:chat-id 79225668,
                    :text    "Куда доставить?",
                    :options {:reply_markup {:resize_keyboard true,
                                             :keyboard        [[{:text "Отправить текущее положение", :request_location true}]]}}},
   :delete-message {:chat-id 79225668, :message-id 10201}})


(deftest request-location-test
  (testing "Test request-location"
    (is (= (order/request-location-handler request-location-ctx)
           request-location-result))))


