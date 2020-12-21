(ns yamfood.telegram.handlers.client.core
  (:require
    [yamfood.utils :as utils]
    [environ.core :refer [env]]
    [yamfood.core.params.core :as p]
    [yamfood.core.bots.core :as bots]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.clients.core :as clients]
    [yamfood.telegram.handlers.utils :as u]))


(defn get-language
  [client]
  (let [supporting [:ru :en :uz]
        client-lang (keyword (get-in client
                                     [:payload :lang]
                                     :ru))]
    (if (utils/in? supporting client-lang)
      client-lang
      :en)))


(defn build-ctx!
  ([token update]
   (let [bot (bots/bot-by-token! token)
         client (clients/client-with-tid!
                  (:id bot)
                  (u/tid-from-update update))]
     {:bot            bot
      :token          token
      :payments-token (:payments_token bot)
      :update         update
      :params         (p/params!)
      :client         client
      :lang           (get-language client)})))


(defn process-message
  [ctx update]
  (let [message (:message update)
        text (:text message)
        contact (:contact message)
        location (:location message)
        reply-to (:reply_to_message message)
        successful_payment (:successful_payment message)]
    (cond
      location (d/dispatch! ctx [:c/location])
      successful_payment (d/dispatch! ctx [:c/successful-payment])
      contact (d/dispatch! ctx [:c/contact])
      reply-to (d/dispatch! ctx [:c/text])
      (and text (re-matches #"^/start(.*)" text)) (d/dispatch! ctx [:c/start])
      text (d/dispatch! ctx [:c/text]))))


(defn client-update-handler!
  [token update]
  (let [message (:message update)
        inline-query (:inline_query update)
        callback-query (:callback_query update)
        pre-checkout-query (:pre_checkout_query update)
        ctx (build-ctx! token update)
        blocked? (:is_blocked (:client ctx))]
    (if (not blocked?)
      (do
        (if message
          (process-message ctx update))
        (if inline-query
          (d/dispatch! ctx [:c/inline]))
        (if pre-checkout-query
          (d/dispatch! ctx [:c/pre-checkout]))
        (if callback-query
          (d/dispatch! ctx [:c/callback])))
      (d/dispatch! ctx [:c/blocked]))))


(defn pre-save-message-id-handler [ctx result]
  {:dispatch {:args        [:c/save-message-id result]
              :rebuild-ctx {:function build-ctx!
                            :update   (:update ctx)
                            :token    (:token ctx)}}})


(defn save-message-id-handler [ctx result]
  (let [client (:client ctx)]
    (clients/update-payload!
      (:id client)
      (assoc (:payload client)
        :last_message_id (:message_id result)))
    {}))


(d/register-event-handler!
  :c/pre-save-message-id
  pre-save-message-id-handler)


(d/register-event-handler!
  :c/save-message-id
  save-message-id-handler)
