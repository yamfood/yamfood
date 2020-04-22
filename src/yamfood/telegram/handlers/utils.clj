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
      [{:text (translate :ru :more-button) :switch_inline_query_current_chat ""}])))


(defn product-not-in-basket-markup
  [state]
  (let [basket-cost (:basket_cost state)]
    {:inline_keyboard
     [[{:text (translate :ru :add-product-button) :callback_data (str "want/" (:id state))}]
      (more-button state)
      [{:text (translate :ru :product-basket-button (fmt-values basket-cost)) :callback_data "basket"}]
      [{:text (translate :ru :product-menu-button) :callback_data "menu"}]]}))


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
      [{:text (translate :ru :product-basket-button (fmt-values basket-cost)) :callback_data "basket"}]
      [{:text (translate :ru :product-menu-button) :callback_data "menu"}]]}))


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
      #(format (str e/food-emoji " %d x %s\n") (:count %) (:name %))
      products)))


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
