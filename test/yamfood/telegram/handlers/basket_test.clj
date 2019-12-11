(ns yamfood.telegram.handlers.basket-test
  (:require
    [clojure.test :refer :all]
    [yamfood.telegram.handlers.basket :as basket]))


(def upd
  {:update_id      435323087,
   :callback_query {:id      "340271655998700705",
                    :from    {:id            79225668,
                              :is_bot        false,
                              :first_name    "Рустам",
                              :last_name     "Бабаджанов",
                              :username      "kensay",
                              :language_code "ru"},
                    :message {:caption      "🥗 Глазунья с болгарским перцем и паштетом \n\n💰15 000 сум  🔋360 кКал",
                              :date         1575898010,
                              :edit_date    1575900824,
                              :chat         {:id         79225668,
                                             :first_name "Рустам",
                                             :last_name  "Бабаджанов",
                                             :username   "kensay",
                                             :type       "private"},
                              :message_id   10147,
                              :photo        [{:file_id   "AgADBAADO6oxG_ZHJVG1o4vuE6D2EHtaqBsABAEAAwIAA20AA30xAwABFgQ",
                                              :file_size 11903, :width 300, :height 300}],
                              :from         {:id         488312680,
                                             :is_bot     true,
                                             :first_name "Kensay",
                                             :username   "kensaybot"},
                              :reply_markup {:inline_keyboard
                                             [[{:text "-", :callback_data "detail-/1"}
                                               {:text "1", :callback_data "nothing"}
                                               {:text "+", :callback_data "detail+/1"}]
                                              [{:text          "Корзина (15 000 сум)",
                                                :callback_data "basket"}]
                                              [{:text                             "Еще!",
                                                :switch_inline_query_current_chat ""}]]}},
                    :data    "basket"}})


(def ctx
  {:token          "488312680:AAGsKHKufV9TQEAB8-g6INps-W82G_noRP8",
   :update         upd
   :payments-token "371317599:TEST:79225668",
   :user           {:id        10,
                    :phone     998909296339,
                    :tid       79225668,
                    :location  {:longitude 34.74037, :latitude 32.020955},
                    :comment   "Test",
                    :basket_id 4}})


(def empty-basket-state
  {:total_cost 0, :total_energy 0, :products ()})


(def result-for-empty-basket-state
  {:send-text      {:chat-id 79225668,
                    :text    "Ваша корзина:",
                    :options {:reply_markup
                              (basket/basket-detail-markup
                                empty-basket-state)}},
   :delete-message {:chat-id 79225668, :message-id 10147}})


(def filled-basket-state
  {:total_cost   28800,
   :total_energy 900,
   :products     [{:id 1, :count 1, :name "Глазунья с болгарским перцем и паштетом", :price 15000, :energy 360}
                  {:id 2, :count 1, :name "Рисовая каша с ежевикой", :price 13800, :energy 540}]})


(def result-for-filled-basket
  {:send-text      {:chat-id 79225668,
                    :text    "Ваша корзина:",
                    :options {:reply_markup
                              (basket/basket-detail-markup
                                filled-basket-state)}}
   :delete-message {:chat-id 79225668, :message-id 10147}})


(def result-for-update-markup-to-empty-basket
  {:edit-reply-markup {:chat_id      79225668,
                       :message_id   10147,
                       :reply_markup (basket/basket-detail-markup empty-basket-state)}})


(def result-for-update-markup-to-filled-basket
  {:edit-reply-markup {:chat_id      79225668,
                       :message_id   10147,
                       :reply_markup (basket/basket-detail-markup filled-basket-state)}})


(def empty-basket-markup
  {:inline_keyboard
   [[{:text "К сожалению, ваша корзина пока пуста :(", :callback_data "nothing"}]
    [{:text "Еще!", :switch_inline_query_current_chat ""}]
    [{:text "💰 0 сум 🔋 0 кКал", :callback_data "nothing"}]
    [{:text "✅ Далее", :callback_data "to-order"}]]})


(def filled-basket-markup
  {:inline_keyboard
   [[{:callback_data "nothing", :text "🥗 1 x Глазунья с болгарским перцем и паштетом"}]
    [{:text "-", :callback_data "basket-/1"}
     {:text "15 000 сум", :callback_data "nothing"}
     {:text "+", :callback_data "basket+/1"}]
    [{:callback_data "nothing", :text "🥗 1 x Рисовая каша с ежевикой"}]
    [{:text "-", :callback_data "basket-/2"}
     {:text "13 800 сум", :callback_data "nothing"}
     {:text "+", :callback_data "basket+/2"}]
    [{:text "Еще!", :switch_inline_query_current_chat ""}]
    [{:text "💰 28 800 сум 🔋 900 кКал", :callback_data "nothing"}]
    [{:text "✅ Далее", :callback_data "to-order"}]]})


(deftest basket-test
  (testing "Test basket-detail-markup with empty basket state"
    (is (= (basket/basket-detail-markup empty-basket-state)
           empty-basket-markup)))

  (testing "Test basket-detail-markup with filled basket state"
    (is (= (basket/basket-detail-markup filled-basket-state)
           filled-basket-markup)))

  (testing "Testing update-basket-markup to empty basket"
    (is (= (basket/update-basket-markup ctx empty-basket-state)
           result-for-update-markup-to-empty-basket)))

  (testing "Testing update-basket-markup to not empty basket"
    (is (= (basket/update-basket-markup ctx filled-basket-state)
           result-for-update-markup-to-filled-basket)))

  (testing "Testing send-basket with full basket"
    (is (= (basket/send-basket ctx filled-basket-state)
           result-for-filled-basket)))

  (testing "Testing send-basket with empty basket"
    (is (= (basket/send-basket ctx empty-basket-state)
           result-for-empty-basket-state))))