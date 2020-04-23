(ns yamfood.telegram.handlers.utils
  (:require
    [clojure.edn :as edn]
    [clojure.string :as str]
    [environ.core :refer [env]]
    [yamfood.telegram.handlers.emojies :as e]
    [yamfood.telegram.translation.core :refer [translate]]))


(def menu-step "menu")
(def phone-step "phone")
(def phone-confirmation-step "phone-confirmation")
(def browse-step "browse")
(def basket-step "basket")
(def order-confirmation-step "order-confirmation")

(def cash-payment "cash")
(def card-payment "card")


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


(defn translated
  [lang map]
  (get map lang "Not translated!!!"))


(defn more-button
  [lang state]
  (let [category (:category state)]
    (if category
      [{:text (str (:emoji state) " " (translated lang category)) :switch_inline_query_current_chat (:emoji state)}]
      [{:text (translate lang :more-button) :switch_inline_query_current_chat ""}])))


(defn product-not-in-basket-markup
  [lang state]
  (let [basket-cost (:basket_cost state)]
    {:inline_keyboard
     [[{:text (translate lang :add-product-button) :callback_data (str "want/" (:id state))}]
      (more-button lang state)
      [{:text (translate lang :product-basket-button (fmt-values basket-cost)) :callback_data "basket"}]
      [{:text (translate lang :product-menu-button) :callback_data "menu"}]]}))


(defn basket-product-controls
  [action-prefix product-id count]
  [{:text "-" :callback_data (str action-prefix "-/" product-id)}
   {:text (str count) :callback_data "nothing"}
   {:text "+" :callback_data (str action-prefix "+/" product-id)}])


(defn product-in-basket-markup
  [lang state]
  (let [basket-cost (:basket_cost state)]
    {:inline_keyboard
     [(basket-product-controls "detail" (:id state) (:count_in_basket state))
      (more-button lang state)
      [{:text (translate lang :product-basket-button (fmt-values basket-cost)) :callback_data "basket"}]
      [{:text (translate lang :product-menu-button) :callback_data "menu"}]]}))


(defn product-detail-markup
  [lang state-for-detail]
  (let [count-in-basket (:count_in_basket state-for-detail)]
    (if (= count-in-basket 0)
      (product-not-in-basket-markup lang state-for-detail)
      (product-in-basket-markup lang state-for-detail))))


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
  ([lang products]
   (doall
     (map
       #(format (str e/food-emoji " %d x %s\n")
                (:count %)
                (translated lang (:name %)))
       products))))


(defn text-from-address
  [address]
  (let [address (:address address)
        road (:road address)
        house (:house_number address)]
    (str road ", " house)))


(defn utm
  [update]
  (let [message (:message update)
        text (:text message)]
    (second (str/split text #" "))))
