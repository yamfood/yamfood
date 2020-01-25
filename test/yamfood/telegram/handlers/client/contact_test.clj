(ns yamfood.telegram.handlers.client.contact-test
  (:require
    [clojure.test :refer :all]
    [yamfood.core.users.core :as users]
    [yamfood.telegram.handlers.client.start :as start]))


(def upd
  {:update_id 435323136,
   :message   {:message_id 10188,
               :from       {:id            79225668,
                            :is_bot        false,
                            :first_name    "Рустам",
                            :last_name     "Бабаджанов",
                            :username      "kensay",
                            :language_code "ru"},
               :chat       {:id         79225668,
                            :first_name "Рустам",
                            :last_name  "Бабаджанов",
                            :username   "kensay",
                            :type       "private"},
               :date       1576093937,
               :contact    {:phone_number "+998909296339",
                            :first_name   "Я",
                            :vcard        "BEGIN:VCARD\nVERSION:3.0\nN:;Я;;;\nFN:Я\nTEL;TYPE=CELL:+998909296339\nEND:VCARD",
                            :user_id      79225668}}})


(def ctx
  {:token          "488312680:AAGsKHKufV9TQEAB8-g6INps-W82G_noRP8",
   :payments-token "371317599:TEST:79225668",
   :update         upd
   :user           nil})


(def contact-handler-result
  {:run       {:function users/create-user!,
               :args     [79225668 998909296339M "Рустам Бабаджанов"]},
   :send-text [{:chat-id 79225668, :options {:reply_markup {:remove_keyboard true}}, :text "Принято!"}
               {:chat-id 79225668,
                :options {:reply_markup {:inline_keyboard [[{:text "Что поесть?", :switch_inline_query_current_chat ""}]
                                                           [{:text "Куда доставляете?",
                                                             :url  "https://test.herokuapp.com/regions"}]]}},
                :text    "Готовим и бесплатно доставляем за 30 минут"}]})


(deftest contact-handler-test
  (testing "Test contact handler with raw update"
    (is (= (start/contact-handler ctx)
           contact-handler-result))))
