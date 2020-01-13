(ns yamfood.telegram.handlers.client.order-test
  (:require
    [clojure.test :refer :all]
    [yamfood.core.orders.core :as ord]
    [yamfood.core.users.core :as users]
    [yamfood.core.baskets.core :as basket]
    [yamfood.core.regions.core :as regions]
    [yamfood.telegram.handlers.client.order :as order]))


(def default-ctx
  {:token          "488312680:AAGsKHKufV9TQEAB8-g6INps-W82G_noRP8",
   :payments-token "371317599:TEST:79225668",
   :update         {}
   :user           {:id        10,
                    :phone     998909296339,
                    :tid       79225668,
                    :location  {:longitude 34.74037, :latitude 32.020955},
                    :comment   "Comment #1",
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
  {:run            {:function   basket/order-confirmation-state!,
                    :args       [4],
                    :next-event :c/send-order-detail},
   :delete-message {:chat-id 79225668, :message-id 10199}})


(def to-order-without-location-result
  {:send-text      {:chat-id 79225668,
                    :text    "Куда доставить?",
                    :options {:reply_markup {:resize_keyboard true,
                                             :keyboard        [[{:text "Отправить текущее положение", :request_location true}]]}},}
   :delete-message {:chat-id 79225668, :message-id 10199}})


(deftest to-order-test
  (testing "Test to-order"
    (is (= (order/to-order-handler to-order-ctx)
           to-order-result)))
  (testing "Test to-order without location"
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
  {:run {:function   regions/region-by-location!,
         :args       [34.740309 32.020991],
         :next-event :c/location}})


(def valid-location-result
  {:send-text {:chat-id 79225668, :text "Локация обновлена", :options {:reply_markup {:remove_keyboard true}}},
   :run       [{:function   basket/order-confirmation-state!
                :args       [4],
                :next-event :c/send-order-detail}
               {:function users/update-location!
                :args     [10 34.740309 32.020991]}]})


(def region-location-result
  {:dispatch {:args [:update-location]}})


(def nil-region-location-result
  {:send-text [{:chat-id 79225668, :text "Ждите...", :options {:reply_markup {:remove_keyboard true}}}
               {:chat-id 79225668,
                :text    "К сожалению, мы не обслуживаем данный регион",
                :options {:reply_markup {:inline_keyboard [[{:text "Карта обслуживания",
                                                             :url  "https://gentle-mesa-91027.herokuapp.com/regions"}]
                                                           [{:text "🧺 Корзина", :callback_data "basket"}]]}}}]})


(deftest location-handler-test
  (testing "Testing location handler"
    (is (= (order/location-handler location-ctx)
           location-result)))
  (testing "Location with region"
    (is (= (order/location-handler location-ctx {:id 1})
           region-location-result)))
  (testing "Location with nil region"
    (is (= (order/location-handler location-ctx nil)
           nil-region-location-result)))
  (testing "Testing valid location"
    (is (= (order/update-location location-ctx)
           valid-location-result))))


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


(def create-order-upd
  {:update_id      435323166,
   :callback_query {:id      "340271655591288140",
                    :from    {:id            79225668,
                              :is_bot        false,
                              :first_name    "Рустам",
                              :last_name     "Бабаджанов",
                              :username      "kensay",
                              :language_code "ru"},
                    :message {:message_id 10208,
                              :from       {:id         488312680,
                                           :is_bot     true,
                                           :first_name "Kensay",
                                           :username   "kensaybot"},
                              :chat       {:id         79225668,
                                           :first_name "Рустам",
                                           :last_name  "Бабаджанов",
                                           :username   "kensay",
                                           :type       "private"},
                              :date       1576243362},
                    :data    "create-order"}})


(def create-order-ctx
  (assoc default-ctx
    :update
    create-order-upd))


(def create-order-result
  {:run             {:function ord/create-order-and-clear-basket!
                     :args     [4 {:longitude 34.74037, :latitude 32.020955} "Comment #1"]},
   :answer-callback {:callback_query_id "340271655591288140",
                     :text              "Ваш заказ успешно создан! Мы будем держать вас в курсе его статуса.",
                     :show_alert        true},
   :dispatch        {:args [:c/order-status]},
   :delete-message  {:chat-id 79225668, :message-id 10208}})


(def raw-order-status-result
  {:run {:function   ord/user-active-order!
         :args       [10],
         :next-event :c/order-status}})


(def active-order
  {:id       24,
   :location {:longitude 32.020991, :latitude 34.740309},
   :comment  "test",
   :products ({:name "Глазунья с болгарским перцем и паштетом", :price 15000, :count 3}
              {:name "Рисовая каша с ежевикой", :price 13800, :count 2})})


(def order-status-result
  {:send-text {:chat-id 79225668,
               :text    (order/order-status-text active-order)
               :options {:parse_mode   "markdown",
                         :reply_markup {:inline_keyboard [[{:text "Оплатить картой", :callback_data "invoice/24"}]]}}}})


(deftest create-order-test
  (testing "create-order-handler"
    (is (= (order/create-order-handler create-order-ctx)
           create-order-result)))
  (testing "raw order-status"
    (is (= (order/order-status create-order-ctx)
           raw-order-status-result)))
  (testing "order-status with active order"
    (is (= (order/order-status create-order-ctx active-order)
           order-status-result))))


(def send-invoice-upd
  {:update_id      435323167,
   :callback_query {:id      "340271653298426848",
                    :from    {:id            79225668,
                              :is_bot        false,
                              :first_name    "Рустам",
                              :last_name     "Бабаджанов",
                              :username      "kensay",
                              :language_code "ru"},
                    :message {:message_id 10209,
                              :from       {:id         488312680,
                                           :is_bot     true,
                                           :first_name "Kensay",
                                           :username   "kensaybot"},
                              :chat       {:id         79225668,
                                           :first_name "Рустам",
                                           :last_name  "Бабаджанов",
                                           :username   "kensay",
                                           :type       "private"},
                              :date       1576243378},
                    :data    "invoice/24"}})


(def send-invoice-ctx
  (assoc default-ctx
    :update
    send-invoice-upd))


(def raw-send-invoice-result
  {:run {:function   ord/user-active-order!,
         :args       [10],
         :next-event :c/send-invoice}})


(def send-invoice-result
  {:send-invoice   {:chat-id     79225668,
                    :title       "Оплатить заказ №24",
                    :description "",
                    :payload     {},
                    :currency    "UZS",
                    :prices      (),
                    :options     {:reply_markup {:inline_keyboard [[{:text "Оплатить", :pay true}]
                                                                   [{:text "Отмена", :callback_data "cancel-invoice"}]]}}},
   :delete-message {:chat-id 79225668, :message-id 10209}})


(deftest send-invoice-test
  (testing "raw send-invoice"
    (is (= (order/send-invoice send-invoice-ctx)
           raw-send-invoice-result)))
  (testing "send-invoice with order"
    (is (= (order/send-invoice send-invoice-ctx active-order)
           send-invoice-result))))


(def cancel-invoice-upd
  {:update_id      435323168,
   :callback_query {:id      "340271654156348694",
                    :from    {:id            79225668,
                              :is_bot        false,
                              :first_name    "Рустам",
                              :last_name     "Бабаджанов",
                              :username      "kensay",
                              :language_code "ru"},
                    :message {:message_id 10210,
                              :from       {:id         488312680,
                                           :is_bot     true,
                                           :first_name "Kensay",
                                           :username   "kensaybot"},
                              :chat       {:id         79225668,
                                           :first_name "Рустам",
                                           :last_name  "Бабаджанов",
                                           :username   "kensay",
                                           :type       "private"},
                              :date       1576243888,
                              :invoice    {:title           "Оплатить заказ №24",
                                           :description     "🥗 3 x Глазунья с болгарским перцем и паштетом\n🥗 2 x Рисовая каша с ежевикой\n",
                                           :start_parameter "test",
                                           :currency        "UZS",
                                           :total_amount    2880000}},
                    :data    "cancel-invoice"}})


(def cancel-invoice-ctx
  (assoc default-ctx
    :update
    cancel-invoice-upd))


(def cancel-invoice-result
  {:dispatch       {:args [:c/order-status]},
   :delete-message {:chat-id 79225668, :message-id 10210}})


(deftest cancel-invoice-test
  (testing "cancel-invoice"
    (is (= (order/cancel-invoice-handler cancel-invoice-ctx)
           cancel-invoice-result))))
