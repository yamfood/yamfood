(ns yamfood.telegram.handlers.client.product-buttons-test
  (:require
    [clojure.test :refer :all]
    [yamfood.core.baskets.core :as baskets]
    [yamfood.telegram.handlers.client.product :as product]))


(def default-ctx
  {:token          "488312680:AAGsKHKufV9TQEAB8-g6INps-W82G_noRP8"
   :payments-token "371317599:TEST:79225668"
   :update         {}
   :user           {:id        10
                    :phone     998909296339
                    :tid       79225668
                    :location  {:longitude 34.74037 :latitude 32.020955}
                    :comment   "Хуй"
                    :basket_id 4}})



(def want-ctx
  (assoc default-ctx
    :update
    {:update_id      435323150
     :callback_query {:id      "340271653139032468"
                      :from    {:id            79225668
                                :is_bot        false
                                :first_name    "Рустам"
                                :last_name     "Бабаджанов"
                                :username      "kensay"
                                :language_code "ru"}
                      :message {:caption          "🥗 Рисовая каша с ежевикой \n\n💰13 800 сум  🔋540 кКал"
                                :date             1576094871
                                :caption_entities [{:offset 3
                                                    :length 23
                                                    :type   "bold"}]
                                :edit_date        1576095142
                                :chat             {:id         79225668
                                                   :first_name "Рустам"
                                                   :last_name  "Бабаджанов"
                                                   :username   "kensay"
                                                   :type       "private"}
                                :message_id       10193
                                :from             {:id         488312680
                                                   :is_bot     true
                                                   :first_name "Kensay"
                                                   :username   "kensaybot"}}
                      :data    "want/2"}}))


(def want-result
  {:run             {:function   baskets/add-product-to-basket!
                     :args       [4 2]
                     :next-event :c/update-markup}
   :answer-callback {:callback_query_id "340271653139032468" :text "Добавлено в корзину"}})


(def product-state
  {:id              2
   :name            "Рисовая каша с ежевикой"
   :price           13800
   :photo           "https://i.ibb.co/cFcyvGD/13-800.png"
   :thumbnail       "https://i.ibb.co/cFcyvGD/13-800.png"
   :energy          540
   :basket_cost     43800
   :count_in_basket 1})


(def update-detail-markup-result
  {:edit-reply-markup
   {:chat_id      79225668
    :message_id   10193
    :reply_markup {:inline_keyboard
                   [[{:text "-" :callback_data "detail-/2"}
                     {:text "1" :callback_data "nothing"}
                     {:text "+" :callback_data "detail+/2"}]
                    [{:text "Корзина (43 800 сум)" :callback_data "basket"}]
                    [{:text "Еще!" :switch_inline_query_current_chat ""}]]}}})


(def detail-inc-ctx
  (assoc default-ctx
    :update
    {:update_id      435323153
     :callback_query {:id            "340271653863261773"
                      :from          {:id            79225668
                                      :is_bot        false
                                      :first_name    "Рустам"
                                      :last_name     "Бабаджанов"
                                      :username      "kensay"
                                      :language_code "ru"}
                      :chat_instance "4402156230761928760"
                      :data          "detail+/2"}}))


(def detail-inc-result
  {:run             {:function   baskets/increment-product-in-basket!
                     :args       [4 2]
                     :next-event :c/update-markup}
   :answer-callback {:callback_query_id "340271653863261773", :text " "}})


(def detail-dec-ctx
  (assoc default-ctx
    :update
    {:update_id      435323153
     :callback_query {:id            "340271653863261773"
                      :from          {:id            79225668
                                      :is_bot        false
                                      :first_name    "Рустам"
                                      :last_name     "Бабаджанов"
                                      :username      "kensay"
                                      :language_code "ru"}
                      :chat_instance "4402156230761928760"
                      :data          "detail-/2"}}))


(def detail-dec-result
  {:run             {:function   baskets/decrement-product-in-basket!
                     :args       [4 2]
                     :next-event :c/update-markup}
   :answer-callback {:callback_query_id "340271653863261773", :text " "}})



(deftest product-buttons-test
  (testing "Test want button handler"
    (is (= (product/want-handler want-ctx) want-result)))
  (testing "Test update-markup"
    (is (= (product/update-detail-markup want-ctx product-state)
           update-detail-markup-result)))
  (testing "Test detail-inc-handler"
    (= (product/detail-inc-handler detail-inc-ctx)
       detail-inc-result))
  (testing "Test detail-dec-handler"
    (= (product/detail-dec-handler detail-dec-ctx)
       detail-dec-result)))

