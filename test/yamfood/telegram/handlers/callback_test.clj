(ns yamfood.telegram.handlers.callback-test
  (:require
    [clojure.test :refer :all]
    [yamfood.telegram.handlers.callback :as callback]))



(def ctx
  {:token          "488312680:AAGsKHKufV9TQEAB8-g6INps-W82G_noRP8",
   :payments-token "371317599:TEST:79225668",
   :update         {}
   :user           {:id        10,
                    :phone     998909296339,
                    :tid       79225668,
                    :location  {:longitude 34.74037, :latitude 32.020955},
                    :comment   "Test",
                    :basket_id 4}})


(def want
  {:ctx    (assoc ctx :update {:callback_query {:data "want/1"}})
   :result {:dispatch {:args [:detail-want]}}})


(def detail-inc
  {:ctx    (assoc ctx :update {:callback_query {:data "detail+/1"}})
   :result {:dispatch {:args [:detail-inc]}}})


(def detail-dec
  {:ctx    (assoc ctx :update {:callback_query {:data "detail-/1"}})
   :result {:dispatch {:args [:detail-dec]}}})


(def basket
  {:ctx    (assoc ctx :update {:callback_query {:data "basket"}})
   :result {:dispatch {:args [:basket]}}})



(def basket-inc
  {:ctx    (assoc ctx :update {:callback_query {:data "basket+/1"}})
   :result {:dispatch {:args [:inc-basket-product]}}})


(def basket-dec
  {:ctx    (assoc ctx :update {:callback_query {:data "basket-/1"}})
   :result {:dispatch {:args [:dec-basket-product]}}})


(def unknown
  {:ctx    (assoc ctx :update {:callback_query {:id   1234
                                                :data "unknown123"}})
   :result {:answer-callback {:callback_query_id 1234
                              :text              " "}}})


(def cases [want
            detail-inc
            detail-dec
            basket
            basket-inc
            basket-dec
            unknown])


(defn execute-case
  [case]
  (= (callback/callback-handler (:ctx case))
     (:result case)))


(deftest callback-handler-test
  (testing "Testing all callback cases"
    (is (every? true? (map execute-case cases)))))


