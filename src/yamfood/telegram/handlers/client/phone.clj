(ns yamfood.telegram.handlers.client.phone
  (:require
    [clojure.string :as str]
    [clojure.spec.alpha :as s]
    [yamfood.core.sms.core :as sms]
    [yamfood.core.specs.core :as cs]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.clients.core :as clients]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.translation.core :refer [translate]]))


(defn generate-confirmation-code
  [digits-count]
  (str/join (map (fn [_]
                   (str (rand-int 10)))
                 (range digits-count))))


(defn request-phone-markup
  [lang]
  {:resize_keyboard true
   :keyboard        [[{:text            (translate lang :send-contact-button)
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
                   :options {:reply_markup (request-phone-markup (:lang ctx))
                             :parse_mode   "markdown"}
                   :text    (translate (:lang ctx) :request-contact-message)}}
      (when query
        {:delete-message {:chat-id    chat-id
                          :message-id (:message_id (:message query))}}))))


(defn phone-confirmation-markup
  [lang]
  {:inline_keyboard
   [[{:text (translate lang :change-phone-button) :callback_data "request-phone"}]]})


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
        lang (:lang ctx)
        client (:client ctx)
        chat-id (u/chat-id update)
        phone (get-phone update)
        code (generate-confirmation-code 4)
        bot-name (:name (:bot ctx))]
    (if phone
      {:run       [{:function sms/create!
                    :args     [phone (translate lang :confirmation-code bot-name code)]}
                   {:function clients/update-payload!
                    :args     [(:id client) (-> (:payload client)
                                                (assoc :unconfirmed-phone phone)
                                                (assoc :step u/phone-confirmation-step)
                                                (assoc :code code))]}]
       :send-text [{:chat-id chat-id
                    :options {:parse_mode   "markdown"
                              :reply_markup {:remove_keyboard true}}
                    :text    (translate lang :accepted)}
                   {:chat-id chat-id
                    :options {:parse_mode   "markdown"
                              :reply_markup (phone-confirmation-markup (:lang ctx))}
                    :text    (translate lang :request-code-message phone)}]}
      {:send-text {:chat-id chat-id
                   :text    (translate lang :invalid-phone-message)}})))


(defn confirm-phone-handler
  [ctx]
  (let [update (:update ctx)
        lang (:lang ctx)
        chat-id (u/chat-id update)
        text (:text (:message update))
        client (:client ctx)
        phone (:unconfirmed-phone (:payload client))
        code (get-in client [:payload :code])
        valid? (= text code)]
    (if valid?
      {:run       {:function clients/update-phone!
                   :args     [(:id client) phone]}
       :send-text {:chat-id chat-id
                   :text    (translate lang :phone-confirmed-message)}
       :dispatch  {:args [:c/menu]}}
      {:send-text {:chat-id chat-id
                   :text    (translate lang :incorrect-code-message)}})))


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
