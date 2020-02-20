(ns yamfood.telegram.handlers.client.start
  (:require
    [environ.core :refer [env]]
    [yamfood.core.users.core :as usr]
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


(def menu-markup
  {:inline_keyboard
   [[{:text                             "Что поесть?"
      :switch_inline_query_current_chat ""}]
    [{:text "Куда доставляете?"
      :url  u/map-url}]]})


(defn menu-handler
  [ctx]
  (let [user (:user ctx)
        message (:message (:update ctx))
        chat-id (:id (:from message))]
    {:send-text {:chat-id chat-id
                 :options {:reply_markup menu-markup}
                 :text    "Готовим и бесплатно доставляем за 30 минут"}
     :run       {:function usr/update-payload!
                 :args     [(:id user)
                            (assoc (:payload user) :step u/menu-step)]}}))


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

