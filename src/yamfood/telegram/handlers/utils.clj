(ns yamfood.telegram.handlers.utils
  (:require [yamfood.core.users.bucket :as b]
            [clojure.string :as str]))


(defn product-not-in-bucket-markup
  [product]
  {:inline_keyboard
   [[{:text "Хочу" :callback_data (str "want/" (:id product))}]
    [{:text "Корзина" :callback_data "basket"}]
    [{:text                             "Еще!"
      :switch_inline_query_current_chat ""}]]})


(defn product-in-bucket-markup
  [product]
  {:inline_keyboard
   [[{:text "-" :callback_data (str "-/" (:id product))}
     {:text (str (:count_in_bucket product)) :callback_data "nothing"}
     {:text "+" :callback_data (str "+/" (:id product))}]
    [{:text "Корзина" :callback_data "basket"}]
    [{:text                             "Еще!"
      :switch_inline_query_current_chat ""}]]})


(defn product-detail-markup
  [product]
  (let [count-in-bucket (:count_in_bucket product)]
    (if (= count-in-bucket 0)
      (product-not-in-bucket-markup product)
      (product-in-bucket-markup product))))


(defn get-callback-action
  [callback-data]
  (first (str/split callback-data #"/")))

(defn get-callback-params
  [callback-data]
  (drop 1 (str/split callback-data #"/")))






