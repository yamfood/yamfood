(ns yamfood.telegram.handlers.client.location
  (:require
    [yamfood.core.users.core :as usr]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.core.regions.core :as regions]))


(def markup-for-request-location
  {:resize_keyboard true
   :keyboard        [[{:text             "Отправить текущее положение"
                       :request_location true}]]})


(defn request-location-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (u/chat-id update)
        message-id (:message_id (:message query))]
    (merge {:send-text {:chat-id chat-id
                        :text    "Куда доставить?"
                        :options {:reply_markup markup-for-request-location}}}
           (when query
             {:delete-message {:chat-id    chat-id
                               :message-id message-id}}))))


(def invalid-location-markup
  {:inline_keyboard
   [[{:text "Карта обслуживания"
      :url  u/map-url}]
    [{:text          (str u/basket-emoji " Корзина")
      :callback_data "basket"}]]})


(defn invalid-location-handler
  [ctx]
  (let [message (:message (:update ctx))
        chat-id (:from (:id message))]
    {:send-text {:chat-id chat-id
                 :text    "К сожалению, мы не обслуживаем данный регион"
                 :options {:reply_markup invalid-location-markup}}}))


(defn location-handler
  ([ctx]
   (let [update (:update ctx)
         message (:message update)
         location (:location message)]
     {:run {:function   regions/region-by-location!
            :args       [(:longitude location)
                         (:latitude location)]
            :next-event :c/location}}))
  ([ctx region]
   (let [user (:user ctx)
         step (:step (:payload user))]
     (if region
       {:dispatch [{:args [:c/update-location]}
                   (cond
                     (= step u/order-confirmation-step) {:args [:c/order-confirmation-state]}
                     (= step u/basket-step) {:args [:c/basket]}
                     :else {:args [:c/menu]})]}
       {:dispatch {:args [:c/invalid-location]}}))))


(defn update-location
  [ctx]
  (let [update (:update ctx)
        message (:message update)
        chat-id (:id (:from message))
        location (:location message)]
    {:run       {:function usr/update-location!
                 :args     [(:id (:user ctx))
                            (:longitude location)
                            (:latitude location)]}
     :send-text {:chat-id chat-id
                 :text    "Локация обновлена"
                 :options {:reply_markup {:remove_keyboard true}}}}))


(d/register-event-handler!
  :c/request-location
  request-location-handler)


(d/register-event-handler!
  :c/location
  location-handler)


(d/register-event-handler!
  :c/update-location
  update-location)


(d/register-event-handler!
  :c/invalid-location
  invalid-location-handler)
