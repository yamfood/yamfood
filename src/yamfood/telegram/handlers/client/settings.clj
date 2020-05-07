(ns yamfood.telegram.handlers.client.settings
  (:require
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.handlers.emojies :as e]
    [yamfood.telegram.translation.core :refer [translate]]
    [yamfood.core.clients.core :as clients]
    [yamfood.telegram.handlers.client.core :as c]))


(defn language-button
  [current-language button-language]
  (let [current? (= current-language button-language)
        mark (when current? "☑️ ")
        data (if current? "nothing" (str "language/" (name button-language)))
        title (get
                {:ru "Русский"
                 :uz "O'zbekcha"
                 :en "English"}
                button-language)]
    {:text          (str mark title)
     :callback_data data}))


(defn settings-markup
  [lang]
  {:inline_keyboard
   [[(language-button lang :ru)
     (language-button lang :uz)
     (language-button lang :en)]
    [{:text          (translate lang :settings-change-phone-button)
      :callback_data "request-phone"}]
    [{:text (translate lang :settings-menu-button) :callback_data "menu"}]]})


(defn settings-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (u/chat-id update)
        message-id (:message_id (:message query))
        lang (:lang ctx)]
    {:edit-message {:chat-id    chat-id
                    :message-id message-id
                    :text       (translate lang :settings-message
                                           (:phone (:client ctx)))
                    :options    {:reply_markup (settings-markup lang)
                                 :parse_mode   "markdown"}}}))


(defn change-language-handler
  [ctx]
  (let [update (:update ctx)
        client (:client ctx)
        query (:callback_query update)
        params (u/callback-params (:data query))
        language (first params)]
    {:run      {:function clients/update-payload!
                :args     [(:id client)
                           (assoc
                             (:payload client)
                             :lang (keyword language))]}
     :dispatch {:args        [:c/settings]
                :rebuild-ctx {:function c/build-ctx!
                              :update   (:update ctx)
                              :token    (:token ctx)}}}))


(d/register-event-handler!
  :c/settings
  settings-handler)


(d/register-event-handler!
  :c/change-language
  change-language-handler)