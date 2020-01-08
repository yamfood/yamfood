(ns yamfood.telegram.handlers.utils
  (:require
    [clojure.string :as str]))


(def location-emoji "\uD83D\uDCCD")
(def money-emoji "\uD83D\uDCB0")
(def comment-emoji "\uD83D\uDCAC")
(def basket-emoji "\uD83E\uDDFA")
(def food-emoji "\uD83E\uDD57")
(def energy-emoji "\uD83D\uDD0B")
(def cancel-emoji "❌")
(def phone-emoji "\uD83D\uDCDE")
(def finish-emoji "✅")
(def client-emoji "\uD83D\uDE03")


; TODO: Make it work with all updates!
(defn tid-from-update
  [update]
  (let [message (:message update)
        callback (:callback_query update)
        inline (:inline_query update)]
    (cond
      message (:id (:from message))
      callback (:id (:from callback))
      inline (:id (:from inline)))))


(defn fmt-values
  [amount]
  (str/replace (format "%,d" amount) "," " "))


(defn product-not-in-basket-markup
  [state]
  (let [basket-cost (:basket_cost state)]
    {:inline_keyboard
     [[{:text "Хочу" :callback_data (str "want/" (:id state))}]
      [{:text (format "Корзина (%s сум)" (fmt-values basket-cost)) :callback_data "basket"}]
      [{:text                             "Еще!"
        :switch_inline_query_current_chat ""}]]}))


(defn basket-product-controls
  [action-prefix product-id count]
  [{:text "-" :callback_data (str action-prefix "-/" product-id)}
   {:text (str count) :callback_data "nothing"}
   {:text "+" :callback_data (str action-prefix "+/" product-id)}])


(defn product-in-basket-markup
  [state]
  (let [basket-cost (:basket_cost state)]
    {:inline_keyboard
     [(basket-product-controls "detail" (:id state) (:count_in_basket state))
      [{:text (format "Корзина (%s сум)" (fmt-values basket-cost)) :callback_data "basket"}]
      [{:text                             "Еще!"
        :switch_inline_query_current_chat ""}]]}))


(defn product-detail-markup
  [state-for-detail]
  (let [count-in-basket (:count_in_basket state-for-detail)]
    (if (= count-in-basket 0)
      (product-not-in-basket-markup state-for-detail)
      (product-in-basket-markup state-for-detail))))


(defn callback-action
  [callback-data]
  (first (str/split callback-data #"/")))


(defn callback-params
  [callback-data]
  (drop 1 (str/split callback-data #"/")))


(defn chat-id
  "Use when you don't know input update type"
  [update]
  (let [message (:message update)
        query (:callback_query update)]
    (cond
      message (:id (:from message))
      query (:id (:from query)))))


(def map-url "https://gentle-mesa-91027.herokuapp.com/regions")


(defn parse-int [s]
  (let [r (re-find #"\d+" s)]
    (when r (Integer. r))))
