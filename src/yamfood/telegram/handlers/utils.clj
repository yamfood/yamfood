(ns yamfood.telegram.handlers.utils
  (:require
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
(def feedback-step "feedback")
(def comment-step "comment")

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
  (or (get map lang)
      (second (first map))))


(defn more-button
  [lang state]
  (let [category (:category state)]
    (if category
      [{:text (str (:emoji state) " " (translated lang category)) :switch_inline_query_current_chat (:emoji state)}]
      [{:text (translate lang :more-button) :switch_inline_query_current_chat ""}])))


(defn navigation-buttons
  [lang state]
  [(more-button lang state)
   [{:text (translate lang :product-basket-button (fmt-values (:basket_cost state))) :callback_data "basket"}]
   [{:text (translate lang :product-menu-button) :callback_data "menu"}]])


(defn product-not-in-basket-markup
  [lang state]
  {:inline_keyboard
   (into
     [[{:text (translate lang :add-product-button) :callback_data (str "want/" (:id state))}]]
     (navigation-buttons lang state))})


(defn basket-product-controls
  [action-prefix product-id count]
  [{:text "-" :callback_data (str action-prefix "-/" product-id)}
   {:text (str count) :callback_data "nothing"}
   {:text "+" :callback_data (str action-prefix "+/" product-id)}])


(defn product-in-basket-markup
  [lang state]
  {:inline_keyboard
   (into
     [(basket-product-controls "detail" (:id state) (:count_in_basket state))]
     (navigation-buttons lang state))})


(defn constructable-product-markup
  [lang state]
  {:inline_keyboard
   (into
     [[{:text (translate lang :construct-product-button) :callback_data (str "construct/" (:id state) "/1")}]]
     (navigation-buttons lang state))})


(defn product-detail-markup
  [lang state]
  (let [constructable? (seq (:modifiers state))
        count-in-basket (:count_in_basket state)]
    (if constructable?
      (constructable-product-markup lang state)
      (if (= count-in-basket 0)
        (product-not-in-basket-markup lang state)
        (product-in-basket-markup lang state)))))


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


(defn from
  "Use when you don't know input update type"
  [update]
  (let [message (:message update)
        inline (:inline_query update)
        query (:callback_query update)]
    (cond
      message (:from message)
      inline (:from inline)
      query (:from query))))


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
  (try
    (let [result (read-string s)]
      (if (number? result)
        result
        nil))
    (catch Exception e
      nil)))


(defn product-modifiers-text
  [lang modifiers]
  (str/join ", " (doall (map #(translated lang (:name %)) modifiers))))


(defn order-one-product-text
  [lang]
  (fn [product]
    (let [count (:count product)
          name (translated lang (:name product))
          modifiers-text (product-modifiers-text lang (:modifiers product))
          modifiers-text (if (seq modifiers-text) (format "(%s)" modifiers-text) modifiers-text)]
      (format (str e/food-emoji " %d x %s %s\n") count name
              (apply str modifiers-text)))))


(defn order-products-text
  ([lang products]
   (doall
     (map
       (order-one-product-text lang)
       products))))


(defn text-from-address
  [address]
  (let [display-name (:display_name address)]
    (str/join ", " (drop-last 3 (str/split display-name #", ")))))


(defn utm
  [update]
  (let [message (:message update)
        text (:text message)]
    (when text (second (str/split text #" ")))))
