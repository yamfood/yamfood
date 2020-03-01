(ns yamfood.telegram.handlers.client.start
  (:require
    [environ.core :refer [env]]
    [yamfood.core.users.core :as usr]
    [yamfood.core.products.core :as p]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.users.core :as users]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.handlers.client.core :as c]))


(defn start-handler
  [ctx]
  (if (and (:user ctx)
           (:phone (:user ctx)))
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
   (let [user (:user ctx)
         message (:message (:update ctx))
         chat-id (:id (:from message))]
     {:send-text {:chat-id chat-id
                  :options {:reply_markup (menu-markup categories)}
                  :text    "Готовим и бесплатно доставляем за 30 минут"}
      :run       {:function usr/update-payload!
                  :args     [(:id user)
                             (assoc (:payload user) :step u/menu-step)]}})))


(defn registration-handler
  [ctx]
  (let [update (:update ctx)
        tid (u/chat-id update)
        from (:from (:message update))
        name (str (:first_name from) " " (:last_name from))]
    (if (:user ctx)
      {:dispatch {:args [:c/request-phone]}}

      {:run      {:function users/create-user!
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
