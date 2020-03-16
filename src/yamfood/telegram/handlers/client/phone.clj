(ns yamfood.telegram.handlers.client.phone
  (:require
    [clojure.spec.alpha :as s]
    [yamfood.core.specs.core :as cs]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.clients.core :as clients]
    [yamfood.telegram.handlers.utils :as u]))


(def request-phone-markup
  {:resize_keyboard true
   :keyboard        [[{:text            "Отправить контакт"
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
                   :text    "Отправь свой контакт или номер телефона в формате _998901234567_\n\n"}}
      (when query
        {:delete-message {:chat-id    chat-id
                          :message-id (:message_id (:message query))}}))))


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
      {:run       [{:function clients/update-payload!
                    :args     [(:id client
                                 (assoc
                                   (:payload client)
                                   :step
                                   u/phone-confirmation-step))]}
                   {:function clients/update-phone!
                    :args     [(:id client) phone]}]
       :send-text {:chat-id chat-id
                   :text    "Номер успешно подтвержден!"}}
      {:send-text {:chat-id chat-id
                   :text    "Неверный номер телефона, попробуйте еще раз..."}})))


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
