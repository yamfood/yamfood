(ns yamfood.telegram.handlers.client.location
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.clients.core :as clients]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.core.regions.core :as regions]
    [yamfood.telegram.handlers.client.core :as c]))


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
  (let [chat-id (u/chat-id (:update ctx))]
    {:send-text [{:chat-id
                  :text "Принято"
                  :options {:reply_markup {:remove_keyboard true}}}
                 {:chat-id chat-id
                  :text    "К сожалению, мы не обслуживаем данный регион"
                  :options {:reply_markup invalid-location-markup}}]}))


(defn location-handler
  ([ctx]
   (let [update (:update ctx)
         message (:message update)
         location (:location message)]
     {:run {:function   regions/location-info!
            :args       [(:longitude location)
                         (:latitude location)]
            :next-event :c/location}}))
  ([ctx location-info]
   (let [client (:client ctx)
         step (:step (:payload client))]
     (if (:region location-info)
       {:dispatch [{:args [:c/update-location location-info]}
                   (cond
                     (= step u/order-confirmation-step) {:args        [:c/order-confirmation-state]
                                                         :rebuild-ctx {:function c/build-ctx!
                                                                       :update   (:update ctx)}}
                     (= step u/basket-step) {:args        [:c/basket]
                                             :rebuild-ctx {:function c/build-ctx!
                                                           :update   (:update ctx)}}
                     :else {:args        [:c/menu]
                            :rebuild-ctx {:function c/build-ctx!
                                          :update   (:update ctx)}})]}
       {:dispatch {:args [:c/invalid-location]}}))))


(defn update-location
  [ctx location-info]
  (let [update (:update ctx)
        client (:client ctx)
        message (:message update)
        chat-id (:id (:from message))
        location (:location message)]
    {:run       {:function clients/update-payload!
                 :args     [(:id client)
                            (assoc
                              (:payload client)
                              :location {:address   (:address location-info)
                                         :longitude (:longitude location)
                                         :latitude  (:latitude location)
                                         :kitchen   (:kitchen location-info)})]}
     :send-text {:chat-id chat-id
                 :text    (str "Новый адресс: " (u/text-from-address (:address location-info)))
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
