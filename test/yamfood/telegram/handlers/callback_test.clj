(ns yamfood.telegram.handlers.callback-test
  (:require
    [clojure.test :refer :all]
    [yamfood.telegram.handlers.callback :as callback]))


(def want
  {:update {:callback_query {:data "want/1"}}
   :result {:dispatch {:args [:detail-want
                              {:callback_query {:data "want/1"}}]}}})


(def detail-inc
  {:update {:callback_query {:data "detail+/1"}}
   :result {:dispatch {:args [:detail-inc
                              {:callback_query {:data "detail+/1"}}]}}})


(def detail-dec
  {:update {:callback_query {:data "detail-/1"}}
   :result {:dispatch {:args [:detail-dec
                              {:callback_query {:data "detail-/1"}}]}}})


(def basket
  {:update {:callback_query {:data "basket"}}
   :result {:dispatch {:args [:basket
                              {:callback_query {:data "basket"}}]}}})



(def basket-inc
  {:update {:callback_query {:data "basket+/1"}}
   :result {:dispatch {:args [:inc-basket-product
                              {:callback_query {:data "basket+/1"}}]}}})


(def basket-dec
  {:update {:callback_query {:data "basket-/1"}}
   :result {:dispatch {:args [:dec-basket-product
                              {:callback_query {:data "basket-/1"}}]}}})


(def unknown
  {:update {:callback_query {:id   1234
                             :data "unknown123"}}
   :result {:answer-callback {:callback_query_id 1234
                              :text              " "}}})


(def cases [want
            detail-inc
            detail-dec
            basket
            basket-inc
            basket-dec
            unknown])


(def ctx
  {:token          "488312680:AAGsKHKufV9TQEAB8-g6INps-W82G_noRP8",
   :payments-token "371317599:TEST:79225668",
   :user           {:id        10,
                    :phone     998909296339,
                    :tid       79225668,
                    :location  {:longitude 34.74037, :latitude 32.020955},
                    :comment   "Test",
                    :basket_id 4}})


(defn execute-case
  [case]
  (= (callback/callback-handler ctx (:update case))
     (:result case)))


(deftest callback-handler-test
  (testing "Testing all callback cases"
    (is (every? true? (map execute-case cases)))))


