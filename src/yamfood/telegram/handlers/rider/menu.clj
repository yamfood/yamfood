(ns yamfood.telegram.handlers.rider.menu
  (:require
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.dispatcher :as d]))


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
   [[{:text (str u/order-emoji " Взять заказ") :switch_inline_query_current_chat ""}]
    [{:text (str u/refresh-emoji " Обновить") :callback_data "refresh-menu"}]]})


(defn rider-menu-handler
  [ctx]
  (let [update (:update ctx)
        rider (:rider ctx)
        chat-id (u/chat-id update)
        utype (u/update-type update)]
    (merge
      {:send-text {:chat-id chat-id
                   :text    (rider-menu-text rider)
                   :options {:reply_markup (rider-menu-markup rider)
                             :parse_mode   "markdown"}}}
      (if (= utype :callback_query) {:delete-message
                                     {:chat-id    chat-id
                                      :message-id (:message_id (:message (:callback_query update)))}}
                                    {}))))


(d/register-event-handler!
  :r/menu
  rider-menu-handler)
