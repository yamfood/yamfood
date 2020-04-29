(ns yamfood.telegram.handlers.client.location
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.clients.core :as clients]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.core.regions.core :as regions]
    [yamfood.telegram.handlers.client.core :as c]
    [yamfood.telegram.translation.core :refer [translate]]))


(defn markup-for-request-location
  [lang]
  {:resize_keyboard true
   :keyboard        [[{:text             (translate lang :send-current-location-button)
                       :request_location true}]]})


(defn request-location-handler
  [ctx]
  (let [update (:update ctx)
        lang (:lang ctx)
        query (:callback_query update)
        chat-id (u/chat-id update)
        message-id (:message_id (:message query))]
    (merge {:send-text {:chat-id chat-id
                        :text    (translate lang :request-location-message)
                        :options {:parse_mode   "markdown"
                                  :reply_markup (markup-for-request-location lang)}}}
           (when query
             {:delete-message {:chat-id    chat-id
                               :message-id message-id}}))))


(defn invalid-location-markup
  [lang]
  {:inline_keyboard
   [[{:text (translate lang :invalid-location-regions-button)
      :url  u/map-url}]
    [{:text          (translate lang :invalid-location-menu-button)
      :callback_data "menu"}]
    [{:text          (translate lang :invalid-location-basket-button)
      :callback_data "basket"}]]})


(defn invalid-location-handler
  [ctx]
  (let [chat-id (u/chat-id (:update ctx))
        lang (:lang ctx)]
    {:send-text [{:chat-id chat-id
                  :text    (translate lang :accepted)
                  :options {:reply_markup {:remove_keyboard true}}}
                 {:chat-id chat-id
                  :text    (translate lang :invalid-location-message)
                  :options {:reply_markup (invalid-location-markup lang)}}]}))


(defn location-handler
  ([ctx]
   (let [update (:update ctx)
         message (:message update)
         location (:location message)]
     {:run {:function   regions/location-info!
            :args       [(:id (:bot ctx))
                         (:longitude location)
                         (:latitude location)]
            :next-event :c/location}}))
  ([ctx location-info]
   (let [client (:client ctx)
         update (:update ctx)
         message (:message update)
         location (:location message)
         step (:step (:payload client))]
     (if (:region location-info)
       {:dispatch [{:args [:c/update-location location-info]}
                   (cond
                     (= step u/order-confirmation-step) {:args        [:c/order-confirmation-state]
                                                         :rebuild-ctx {:function c/build-ctx!
                                                                       :update   (:update ctx)
                                                                       :token    (:token ctx)}}
                     (= step u/basket-step) {:args        [:c/basket]
                                             :rebuild-ctx {:function c/build-ctx!
                                                           :update   (:update ctx)
                                                           :token    (:token ctx)}}
                     :else {:args        [:c/menu]
                            :rebuild-ctx {:function c/build-ctx!
                                          :update   (:update ctx)
                                          :token    (:token ctx)}})]}
       {:run      {:function clients/update-payload!
                   :args     [(:id client)
                              (assoc
                                (:payload client)
                                :invalid-location {:address   (:address location-info)
                                                   :longitude (:longitude location)
                                                   :latitude  (:latitude location)})]}
        :dispatch {:args [:c/invalid-location]}}))))


(defn update-location
  [ctx location-info]
  (let [update (:update ctx)
        lang (:lang ctx)
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
                 :text    (translate lang :new-location-message (u/text-from-address (:address location-info)))
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
