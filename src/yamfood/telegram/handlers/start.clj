(ns yamfood.telegram.handlers.start
  (:require [yamfood.core.users.core :as users]
            [environ.core :refer [env]]))


(def menu-markup
  {:inline_keyboard
   [[{:text                             "test"
      :switch_inline_query_current_chat ""}]]})


(defn menu-message
  [message]
  (let [chat (:chat message)
        chat-id (:id chat)]
    {:send-text
     {:chat-id chat-id
      :options {:reply_markup menu-markup}
      :text    "Welcome"}}))


(def registration-markup
  {:resize_keyboard true
   :keyboard        [[{:text            "Отправить контакт"
                       :request_contact true}]]})


(defn init-registration
  [message]
  {:send-text {:chat-id (:id (:chat message))
               :options {:reply_markup registration-markup}
               :text    "Отправьте свой контакт"}})


(defn parse-int [s]
  (bigdec (re-find #"\d+" s)))


(defn handle-contact
  [_ update]
  (let [message (:message update)
        contact (:contact message)
        phone (parse-int (:phone_number contact))
        tid (:id (:from message))]
    {:core {:fn #(users/create-user! tid phone)}
     :send-text [{:chat-id (:id (:chat message))
                  :options {:reply_markup {:remove_keyboard true}}
                  :text    "Принято!"}
                 (:send-text (menu-message message))]}))


(defn handle-start
  [ctx update]
  (let [message (:message update)]
    (if (:user ctx)
      (menu-message message)
      (init-registration message))))

