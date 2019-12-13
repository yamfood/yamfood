(ns yamfood.telegram.handlers.order-test
  (:require
    [clojure.test :refer :all]
    [yamfood.core.baskets.core :as basket]
    [yamfood.telegram.handlers.order :as order]
    [yamfood.core.users.core :as users]))


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


(def pre-order-state
  {:basket {:total_cost   72600,
            :total_energy 2160,
            :products     ({:id 1, :count 3, :name "Глазунья с болгарским перцем и паштетом", :price 15000, :energy 360}
                           {:id 2, :count 2, :name "Рисовая каша с ежевикой", :price 13800, :energy 540})},
   :user   {:id        10,
            :phone     998909296339,
            :tid       79225668,
            :location  {:longitude 34.740309, :latitude 32.020991},
            :comment   "Тест",
            :basket_id 4}})


(def order-detail-handler-result
  {:send-text {:chat-id 79225668,
               :text    (order/pre-order-text pre-order-state)
               :options {:reply_markup {:inline_keyboard [[{:text "📍", :callback_data "request-location"}
                                                           {:text "💬", :callback_data "change-comment"}]
                                                          [{:text "🧺 Корзина", :callback_data "basket"}]
                                                          [{:text "✅ Подтвердить", :callback_data "create-order"}]]},
                         :parse_mode   "markdown"}}})


(deftest order-detail-handler-test
  (testing "Testing order-detail-handler"
    (is (= (order/order-detail-handler to-order-ctx pre-order-state)
           order-detail-handler-result))))


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


(def upd-with-location
  {:update_id 435323163,
   :message   {:message_id       10203,
               :from             {:id            79225668,
                                  :is_bot        false,
                                  :first_name    "Рустам",
                                  :last_name     "Бабаджанов",
                                  :username      "kensay",
                                  :language_code "ru"},
               :chat             {:id         79225668,
                                  :first_name "Рустам",
                                  :last_name  "Бабаджанов",
                                  :username   "kensay",
                                  :type       "private"},
               :date             1576242412,
               :reply_to_message {:message_id 10202,
                                  :from       {:id         488312680,
                                               :is_bot     true,
                                               :first_name "Kensay",
                                               :username   "kensaybot"},
                                  :chat       {:id         79225668,
                                               :first_name "Рустам",
                                               :last_name  "Бабаджанов",
                                               :username   "kensay",
                                               :type       "private"},
                                  :date       1576242194,
                                  :text       "Куда доставить?"},
               :location         {:latitude  32.020991,
                                  :longitude 34.740309}}})


(def location-ctx
  (assoc default-ctx
    :update
    upd-with-location))


(def location-result
  {:send-text {:chat-id 79225668, :text "Локация обновлена", :options {:reply_markup {:remove_keyboard true}}},
   :run       [{:function   basket/pre-order-state!
                :args       [4],
                :next-event :send-order-detail}
               {:function users/update-location!
                :args     [10 34.740309 32.020991]}]})


(deftest location-handler-test
  (testing "Testing location handler"
    (is (= (order/location-handler location-ctx)
           location-result))))


(def change-comment-upd
  {:update_id      435323164,
   :callback_query {:id      "340271657069740466",
                    :from    {:id            79225668,
                              :is_bot        false,
                              :first_name    "Рустам",
                              :last_name     "Бабаджанов",
                              :username      "kensay",
                              :language_code "ru"},
                    :message {:message_id 10205,
                              :from       {:id         488312680,
                                           :is_bot     true,
                                           :first_name "Kensay",
                                           :username   "kensaybot"},
                              :chat       {:id         79225668,
                                           :first_name "Рустам",
                                           :last_name  "Бабаджанов",
                                           :username   "kensay",
                                           :type       "private"},
                              :date       1576242419},
                    :data    "change-comment"}})


(def change-comment-ctx
  (assoc default-ctx
    :update
    change-comment-upd))


(def change-comment-result
  {:send-text      {:chat-id 79225668,
                    :text    "Напишите свой комментарий к заказу",
                    :options {:reply_markup {:force_reply true}}},
   :delete-message {:chat-id 79225668, :message-id 10205}})


(deftest change-comment-handler-test
  (testing "change-comment-handler"
    (is (= (order/change-comment-handler change-comment-ctx)
           change-comment-result))))
