(ns yamfood.telegram.handlers.client.phone
  (:require
    [clojure.spec.alpha :as s]
    [yamfood.core.specs.core :as cs]
    [yamfood.core.users.core :as usr]
    [yamfood.telegram.dispatcher :as d]
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
        user (:user ctx)]
    (merge
      {:run       {:function usr/update-payload!
                   :args     [(:id user)
                              (assoc
                                (:payload user)
                                :step u/phone-step)]}
       :send-text {:chat-id chat-id
                   :options {:reply_markup request-phone-markup
                             :parse_mode   "markdown"}
                   :text    (str "Отправь свой контакт или номер телефона в формате _998901234567_\n\n"
                                 "Мы отправим смс с кодом для подтверждения")}}
      (when query
        {:delete-message {:chat-id    chat-id
                          :message-id (:message_id (:message query))}}))))


(def phone-confirmation-markup
  {:inline_keyboard
   [[{:text "Изменить номер" :callback_data "request-phone"}]]})


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
        user (:user ctx)
        chat-id (u/chat-id update)
        phone (get-phone update)]
    (if phone
      {:run       {:function usr/update-payload!
                   :args     [(:id user)
                              (merge
                                (assoc
                                  (:payload user)
                                  :unconfirmed-phone
                                  phone)
                                (assoc
                                  (:payload user)
                                  :step
                                  u/phone-confirmation-step))]}
       :send-text [{:chat-id chat-id
                    :options {:parse_mode   "markdown"
                              :reply_markup {:remove_keyboard true}}
                    :text    "Принято"}
                   {:chat-id chat-id
                    :options {:parse_mode   "markdown"
                              :reply_markup phone-confirmation-markup}
                    :text    (format "Отправьте 4х значный код отправленный на номер _+%s_"
                                     phone)}]}
      {:send-text {:chat-id chat-id
                   :text    "Неверный номер телефона, попробуйте еще раз..."}})))


(defn confirm-phone-handler
  [ctx]
  (let [update (:update ctx)
        chat-id (u/chat-id update)
        text (:text (:message update))
        user (:user ctx)
        phone (:unconfirmed-phone (:payload user))
        valid? (= text "0000")]
    (if valid?
      {:run       {:function usr/update-phone!
                   :args     [(:id user) phone]}
       :send-text {:chat-id chat-id
                   :text    "Номер успешно подтвержден!"}
       :dispatch  {:args [:c/menu]}}
      {:send-text {:chat-id chat-id
                   :text    "Неверный код, попробуйте еще раз."}})))


(defn contact-handler
  [ctx]
  (let [step (:step (:payload (:user ctx)))
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
