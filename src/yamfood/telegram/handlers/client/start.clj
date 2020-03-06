(ns yamfood.telegram.handlers.client.start
  (:require
    [environ.core :refer [env]]
    [yamfood.core.products.core :as p]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.clients.core :as clients]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.handlers.client.core :as c]))


(defn start-handler
  [ctx]
  (if (and (:client ctx)
           (:phone (:client ctx)))
    {:dispatch {:args [:c/menu]}}
    {:dispatch {:args [:c/registration]}}))


(defn categories-reducer
  [r category]
  (let [current (last r)
        c (count current)
        emoji (:emoji category)
        text (str emoji " " (:name category))
        next {:text text :switch_inline_query_current_chat emoji}]
    (cond
      (= c 1) (conj (pop r) [(first current) next])
      :else (conj r [next]))))


(defn categories-in-markup
  [categories]
  (reduce categories-reducer [] categories))


(defn menu-markup
  [categories]
  (let []
    {:inline_keyboard
     (conj (apply conj []
                  [{:text                             "Что поесть?"
                    :switch_inline_query_current_chat ""}]
                  (categories-in-markup categories))
           [{:text (str u/location-emoji " Зона покрытия")
             :url  u/map-url}]
           [{:text          (str u/settings-emoji " Настройки")
             :callback_data "settings"}])}))


(defn menu-handler
  ([_]
   {:run {:function   p/all-categories!
          :args       []
          :next-event :c/menu}})
  ([ctx categories]
   (let [client (:client ctx)
         update (:update ctx)
         chat-id (u/chat-id update)
         query (:callback_query update)]
     (merge
       {:send-text {:chat-id chat-id
                    :options {:reply_markup (menu-markup categories)}
                    :text    "Готовим и бесплатно доставляем за 30 минут"}
        :run       {:function clients/update-payload!
                    :args     [(:id client)
                               (assoc (:payload client) :step u/menu-step)]}}
       (when query
         {:delete-message {:chat-id    chat-id
                           :message-id (:message_id (:message query))}})))))


(defn registration-handler
  [ctx]
  (let [update (:update ctx)
        tid (u/chat-id update)
        from (:from (:message update))
        name (str (:first_name from) " " (:last_name from))]
    (if (:client ctx)
      {:dispatch {:args [:c/request-phone]}}

      {:run      {:function clients/create-client!
                  :args     [tid name]}
       :dispatch {:args        [:c/request-phone]
                  :rebuild-ctx {:function c/build-ctx!
                                :update   (:update ctx)}}})))


(d/register-event-handler!
  :c/start
  start-handler)


(d/register-event-handler!
  :c/registration
  registration-handler)


(d/register-event-handler!
  :c/menu
  menu-handler)
