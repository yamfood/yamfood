(ns yamfood.telegram.handlers.utils
  (:require [clojure.string :as str]))


(defn product-not-in-bucket-markup
  [state]
  (let [bucket-cost (:bucket_cost state)]
    {:inline_keyboard
     [[{:text "Хочу" :callback_data (str "want/" (:id state))}]
      [{:text (format "Корзина (%,d сум.)" bucket-cost) :callback_data "bucket"}]
      [{:text                             "Еще!"
        :switch_inline_query_current_chat ""}]]}))

(defn bucket-product-controls
  [action-prefix product-id count]
  [{:text "-" :callback_data (str action-prefix "-/" product-id)}
   {:text (str count) :callback_data "nothing"}
   {:text "+" :callback_data (str action-prefix "+/" product-id)}])

(defn product-in-bucket-markup
  [state]
  (let [bucket-cost (:bucket_cost state)]
    {:inline_keyboard
     [(bucket-product-controls "detail" (:id state) (:count_in_bucket state))
      [{:text (format "Корзина (%,d сум.)" bucket-cost) :callback_data "bucket"}]
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





