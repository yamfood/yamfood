(ns yamfood.telegram.dispatcher-test
  (:require [clojure.test :refer :all]
            [yamfood.telegram.dispatcher :as d]))


(deftest register-event-handler-test
  (testing "Testing register-event-handler!"
    (d/register-event-handler!
      :test
      (fn []))
    (is (= (contains? @d/event-handlers :test) true))))


(deftest register-effect-handler-test
  (testing "Testing register-effect-handler!"
    (d/register-effect-handler!
      :test
      (fn []))
    (is (= (contains? @d/effect-handlers :test) true))))


(deftest dispatch-test
  (testing "Testing dispatch! function"
    (d/register-event-handler!
      :test
      (fn [ctx update]
        {:test {:update update}}))

    (d/register-effect-handler!
      :test
      (fn [ctx eff]
        {:ctx ctx :effect eff}))
    (let [ctx {:user "test"}
          update {:test "update"}
          result (first (d/dispatch! ctx [:test update]))]
      (is (= result {:ctx ctx :effect {:update update}})))))

