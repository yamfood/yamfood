(ns yamfood.telegram.handlers.utils
  (:require
    [clojure.string :as str]
    [environ.core :refer [env]]
    [clojure.edn :as edn]))


(def location-emoji "\uD83D\uDCCD")
(def money-emoji "\uD83D\uDCB0")
(def cash-emoji "\uD83D\uDCB5")
(def card-emoji "\uD83D\uDCB3")
(def comment-emoji "\uD83D\uDCAC")
(def basket-emoji "\uD83E\uDDFA")
(def food-emoji "\uD83E\uDD57")
(def energy-emoji "\uD83D\uDD0B")
(def cancel-emoji "❌")
(def phone-emoji "\uD83D\uDCDE")
(def finish-emoji "✅")
(def client-emoji "\uD83D\uDE03")
(def order-emoji "\uD83D\uDDD2️")
(def refresh-emoji "\uD83D\uDD04️")
(def settings-emoji "⚙️")
(def back-emoji "⬅️")

(def menu-step "menu")
(def phone-step "phone")
(def phone-confirmation-step "phone-confirmation")
(def browse-step "browse")
(def basket-step "basket")
(def order-confirmation-step "order-confirmation")

(def cash-payment {:value "cash"
                   :label "Наличными"})
(def card-payment {:value "card"
                   :label "Картой"})


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


(defn more-button
  [state]
  (let [category (:category state)]
    (if category
      [{:text (str (:emoji state) " " category) :switch_inline_query_current_chat (:emoji state)}]
      [{:text "\uD83C\uDF7D Еще?" :switch_inline_query_current_chat ""}])))


(defn product-not-in-basket-markup
  [state]
  (let [basket-cost (:basket_cost state)]
    {:inline_keyboard
     [[{:text "Хочу" :callback_data (str "want/" (:id state))}]
      (more-button state)
      [{:text (format (str basket-emoji " Корзина (%s сум)") (fmt-values basket-cost)) :callback_data "basket"}]]}))


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
      (more-button state)
      [{:text (format (str basket-emoji " Корзина (%s сум)") (fmt-values basket-cost)) :callback_data "basket"}]]}))


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
        inline (:inline_query update)
        query (:callback_query update)]
    (cond
      message (:id (:from message))
      inline (:id (:from inline))
      query (:id (:from query)))))


(defn update-type
  [update]
  (let [message (:message update)
        callback-query (:callback_query update)
        inline-query (:inline_query update)]
    (cond
      message :message
      callback-query :callback_query
      inline-query :inline_query)))


(def map-url
  (format "https://%s.herokuapp.com/regions"
          (or (env :app-name) "test")))


(defn parse-int [s]
  (edn/read-string s))


(defn order-products-text
  [products]
  (doall
    (map
      #(format (str food-emoji " %d x %s\n") (:count %) (:name %))
      products)))


(defn text-from-address
  [address]
  (let [address (:address address)
        road (:road address)
        house (:house_number address)]
    (str road ", " house)))
