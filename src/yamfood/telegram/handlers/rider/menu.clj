(ns yamfood.telegram.handlers.rider.menu
  (:require
    [yamfood.core.riders.core :as r]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.handlers.emojies :as e]
    [yamfood.telegram.handlers.rider.core :as c]))


(defn rider-menu-text
  [state]
  (format (str "*[%s]* %s\n\n"
               "*Сегодня:*\n"
               "  Завершил: %s заказов\n"
               "  Заработал: %s сум\n\n"
               "*Депозит:* %s сум")
          (:id state)
          (:name state)
          (:finished-orders-today state)
          (u/fmt-values (:earned-money-today state))
          (u/fmt-values (:deposit state))))


(def rider-menu-markup
  {:inline_keyboard
   [[{:text (str e/refresh-emoji " Обновить") :callback_data "refresh-menu"}]]})


(defn rider-menu-handler
  ([ctx]
   (let [rider (:rider ctx)
         delivery-cost (:delivery-cost (:params ctx))]
     (if (:id rider)
       {:run {:function   r/menu-state!
              :args       [(:id rider) delivery-cost]
              :next-event :r/menu}}
       {:dispatch {:args [:r/registration]}})))
  ([ctx menu-state]
   (let [update (:update ctx)
         chat-id (u/chat-id update)
         utype (u/update-type update)]
     (merge
       {:send-text {:chat-id chat-id
                    :text    (rider-menu-text menu-state)
                    :options {:reply_markup rider-menu-markup
                              :parse_mode   "markdown"}}}
       (if (= utype :callback_query)
         {:delete-message
          {:chat-id    chat-id
           :message-id (:message_id (:message (:callback_query update)))}}
         {})))))


(defn refresh-menu-handler
  ([ctx]
   (let [delivery-cost (:delivery-cost (:params ctx))]
     {:run {:function   r/menu-state!
            :args       [(:id (:rider ctx)) delivery-cost]
            :next-event :r/refresh-menu}}))
  ([ctx state]
   (let [update (:update ctx)
         chat-id (u/chat-id update)
         query (:callback_query update)
         message-id (:message_id (:message query))]
     {:edit-message    {:chat-id    chat-id
                        :message-id message-id
                        :text       (rider-menu-text state)
                        :options    {:parse_mode   "markdown"
                                     :reply_markup rider-menu-markup}}
      :answer-callback {:callback_query_id (:id query)
                        :text              " "}})))


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
  :r/refresh-menu
  refresh-menu-handler)


(d/register-event-handler!
  :r/registration
  registration-handler)


(d/register-event-handler!
  :r/contact
  contact-handler)