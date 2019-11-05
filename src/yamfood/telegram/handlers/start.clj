(ns yamfood.telegram.handlers.start
  (:require [morse.api :as t]
            [yamfood.core.users.core :as users]
            [environ.core :refer [env]]))


(def menu-markup
  {:reply_markup {:inline_keyboard [[{:text                             "test"
                                      :switch_inline_query_current_chat ""}]]}})

(defn send-menu
  [ctx message]
  (let [chat (:chat message)
        chat-id (:id chat)]
    (t/send-text
      (:token ctx)
      chat-id
      menu-markup
      "Welcome")))


(def registration-markup
  {:resize_keyboard true
   :reply_markup    {:keyboard [[{:text            "Отправить контакт"
                                  :request_contact true}]]}})

(defn init-registration
  [ctx message]
  (t/send-text (:token ctx) (:id (:chat message))
               registration-markup
               "Отправьте свой контакт"))


(defn create-user
  [tid phone]
  (users/create-user! tid phone))


(defn parse-int [s]
  (bigdec (re-find #"\d+" s)))


(defn handle-contact
  [ctx update]
  (let [message (:message update)
        contact (:contact message)
        phone (parse-int (:phone_number contact))
        tid (:id (:from message))]
    (create-user tid phone)
    (t/send-text (:token ctx) (:id (:chat message))
                 {:reply_markup {:remove_keyboard true}}
                 "Принято!")
    (send-menu ctx message)))


(defn handle-start
  [ctx update]
  (let [message (:message update)]
    (if (:user ctx)
      (send-menu ctx message)
      (init-registration ctx message))))

