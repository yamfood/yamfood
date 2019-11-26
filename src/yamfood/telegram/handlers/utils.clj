(ns yamfood.telegram.handlers.utils
  (:require [yamfood.core.users.bucket :as b]
            [clojure.string :as str]))


(defn product-not-in-bucket-markup
  [state]
  (let [positions-count (:positions_in_bucket state)]
    {:inline_keyboard
     [[{:text "Хочу" :callback_data (str "want/" (:id state))}]
      [{:text (format "Корзина (%d)" positions-count) :callback_data "basket"}]
      [{:text                             "Еще!"
        :switch_inline_query_current_chat ""}]]}))


(defn product-in-bucket-markup
  [state]
  (let [positions-count (:positions_in_bucket state)]
    {:inline_keyboard
     [[{:text "-" :callback_data (str "-/" (:id state))}
       {:text (str (:count_in_bucket state)) :callback_data "nothing"}
       {:text "+" :callback_data (str "+/" (:id state))}]
      [{:text (format "Корзина (%d)" positions-count) :callback_data "basket"}]
      [{:text                             "Еще!"
        :switch_inline_query_current_chat ""}]]}))


(defn product-detail-markup
  [state-for-detail]
  (let [count-in-bucket (:count_in_bucket state-for-detail)]
    (if (= count-in-bucket 0)
      (product-not-in-bucket-markup state-for-detail)
      (product-in-bucket-markup state-for-detail))))


(defn get-callback-action
  [callback-data]
  (first (str/split callback-data #"/")))

(defn get-callback-params
  [callback-data]
  (drop 1 (str/split callback-data #"/")))






