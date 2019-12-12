(ns yamfood.telegram.handlers.basket-buttons-test
  (:require
    [clojure.test :refer :all]
    [yamfood.core.baskets.core :as baskets]
    [yamfood.telegram.handlers.basket :as basket]))


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


(def basket-inc-ctx
  (assoc default-ctx
    :update
    {:update_id      435323155
     :callback_query {:id   "340271655032893460"
                      :from {:id            79225668
                             :is_bot        false
                             :first_name    "Рустам"
                             :last_name     "Бабаджанов"
                             :username      "kensay"
                             :language_code "ru"}
                      :data "basket+/1"}}))


(def basket-dec-ctx
  (assoc default-ctx
    :update
    {:update_id      435323155
     :callback_query {:id   "340271655032893460"
                      :from {:id            79225668
                             :is_bot        false
                             :first_name    "Рустам"
                             :last_name     "Бабаджанов"
                             :username      "kensay"
                             :language_code "ru"}
                      :data "basket-/1"}}))


(def basket-inc-result
  {:run             [{:function baskets/increment-product-in-basket!
                      :args     [4 1]}
                     {:function   baskets/basket-state!
                      :args       [4]
                      :next-event :update-basket-markup}]
   :answer-callback {:callback_query_id "340271655032893460" :text " "}})


(def basket-dec-result
  {:run             [{:function baskets/decrement-product-in-basket!
                      :args     [4 1]}
                     {:function   baskets/basket-state!
                      :args       [4]
                      :next-event :update-basket-markup}]
   :answer-callback {:callback_query_id "340271655032893460" :text " "}})


(deftest basket-callbacks-test
  (testing "Testing basket-inc-handler"
    (is (= (basket/basket-inc-handler basket-inc-ctx)
           basket-inc-result)))
  (testing "Testing basket-dec-handler"
    (is (= (basket/basket-dec-handler basket-dec-ctx)
           basket-dec-result))))
