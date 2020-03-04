(ns yamfood.telegram.handlers.rider.menu
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.core.riders.core :as r]
    [yamfood.telegram.handlers.rider.core :as c]))


(defn rider-menu-text
  [rider]
  (str "*[3312]* Бабаджанов Рустам\n\n"
       "*Сегодня:*\n"
       "  Завершил: 13 заказов\n"
       "  Заработал: 35 000 сум\n\n"
       "*Должен сдать:* 167 000 сум"))


(defn rider-menu-markup
  [rider]
  {:inline_keyboard
   [(if (:active-order rider)
      [{:text (str u/order-emoji " Текущий заказ") :callback_data (str "order-detail/" (:id (:active-order rider)))}]
      [{:text (str u/order-emoji " Взять заказ") :switch_inline_query_current_chat ""}])
    [{:text (str u/refresh-emoji " Обновить") :callback_data "refresh-menu"}]]})


(defn rider-menu-handler
  [ctx]
  (let [update (:update ctx)
        rider (:rider ctx)
        chat-id (u/chat-id update)
        utype (u/update-type update)]
    (if (:id rider)
      (merge
        {:send-text {:chat-id chat-id
                     :text    (rider-menu-text rider)
                     :options {:reply_markup (rider-menu-markup rider)
                               :parse_mode   "markdown"}}}
        (if (= utype :callback_query)
          {:delete-message
           {:chat-id    chat-id
            :message-id (:message_id (:message (:callback_query update)))}}
          {}))
      {:dispatch {:args [:r/registration]}})))


(def registration-markup
  {:keyboard        [[{:text "Отправить контакт" :request_contact true}]]
   :resize_keyboard true})


(defn registration-handler
  [ctx]
  (let [update (:update ctx)
        chat-id (u/chat-id update)]
    {:send-text {:chat-id chat-id
                 :text    "Отправьте свой контакт"
                 :options {:reply_markup registration-markup}}}))


(defn contact-handler
  [ctx]
  (let [update (:update ctx)
        chat-id (u/chat-id update)
        contact (:contact (:message update))
        phone (u/parse-int (:phone_number contact))
        rider (r/rider-by-phone! phone)]
    (if rider
      (do
        (r/update! (:id rider) {:tid chat-id})
        {:send-text {:chat-id chat-id
                     :text    "Принято"
                     :options {:reply_markup {:remove_keyboard true}}}
         :dispatch  {:args        [:r/menu]
                     :rebuild-ctx {:function c/build-ctx!
                                   :update   update}}}))))


(d/register-event-handler!
  :r/menu
  rider-menu-handler)


(d/register-event-handler!
  :r/registration
  registration-handler)


(d/register-event-handler!
  :r/contact
  contact-handler)
