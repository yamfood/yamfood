(ns yamfood.telegram.handlers.client.phone
  (:require
    [clojure.spec.alpha :as s]
    [yamfood.core.specs.core :as cs]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.clients.core :as clients]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.translation.core :refer [translate]]))


(def request-phone-markup
  {:resize_keyboard true
   :keyboard        [[{:text            (translate :ru :send-contact-button)
                       :request_contact true}]]})


(defn request-phone
  [ctx]
  (let [update (:update ctx)
        chat-id (u/chat-id update)
        query (:callback_query update)
        client (:client ctx)]
    (merge
      {:run       {:function clients/update-payload!
                   :args     [(:id client)
                              (assoc
                                (:payload client)
                                :step u/phone-step)]}
       :send-text {:chat-id chat-id
                   :options {:reply_markup request-phone-markup
                             :parse_mode   "markdown"}
                   :text    (translate :ru :request-contact-message)}}
      (when query
        {:delete-message {:chat-id    chat-id
                          :message-id (:message_id (:message query))}}))))


(def phone-confirmation-markup
  {:inline_keyboard
   [[{:text (translate :ru :change-phone-button) :callback_data "request-phone"}]]})


(defn get-phone
  [update]
  (let [message (:message update)
        contact (:contact message)
        phone (u/parse-int (if contact
                             (:phone_number contact)
                             (:text message)))]
    (when (s/valid? ::cs/phone phone)
      phone)))


(defn phone-handler
  [ctx]
  (let [update (:update ctx)
        client (:client ctx)
        chat-id (u/chat-id update)
        phone (get-phone update)]
    (if phone
      {:run       {:function clients/update-payload!
                   :args     [(:id client)
                              (merge
                                (assoc
                                  (:payload client)
                                  :unconfirmed-phone
                                  phone)
                                (assoc
                                  (:payload client)
                                  :step
                                  u/phone-confirmation-step))]}
       :send-text [{:chat-id chat-id
                    :options {:parse_mode   "markdown"
                              :reply_markup {:remove_keyboard true}}
                    :text    (translate :ru :accepted)}
                   {:chat-id chat-id
                    :options {:parse_mode   "markdown"
                              :reply_markup phone-confirmation-markup}
                    :text    (translate :ru :request-code-message phone)}]}
      {:send-text {:chat-id chat-id
                   :text    (translate :ru :invalid-phone-message)}})))


(defn confirm-phone-handler
  [ctx]
  (let [update (:update ctx)
        chat-id (u/chat-id update)
        text (:text (:message update))
        client (:client ctx)
        phone (:unconfirmed-phone (:payload client))
        valid? (= text "0000")]
    (if valid?
      {:run       {:function clients/update-phone!
                   :args     [(:id client) phone]}
       :send-text {:chat-id chat-id
                   :text    (translate :ru :phone-confirmed-message)}
       :dispatch  {:args [:c/menu]}}
      {:send-text {:chat-id chat-id
                   :text    (translate :ru :incorrect-code-message)}})))


(defn contact-handler
  [ctx]
  (let [step (:step (:payload (:client ctx)))
        chat-id (u/chat-id (:update ctx))]
    (if (= step u/phone-step)
      {:dispatch {:args [:c/phone]}}
      {:send-text {:chat-id chat-id
                   :text    step}})))


(d/register-event-handler!
  :c/contact
  contact-handler)


(d/register-event-handler!
  :c/request-phone
  request-phone)


(d/register-event-handler!
  :c/phone
  phone-handler)


(d/register-event-handler!
  :c/confirm-phone
  confirm-phone-handler)
