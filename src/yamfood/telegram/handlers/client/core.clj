(ns yamfood.telegram.handlers.client.core
  (:require
    [environ.core :refer [env]]
    [yamfood.core.users.core :as users]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn build-ctx!
  [update]
  {:token          (env :bot-token)
   :payments-token (env :payments-token)
   :update         update
   :user           (users/user-with-tid!
                     (u/tid-from-update update))})


(defn process-message
  [ctx update]
  (let [message (:message update)
        text (:text message)
        contact (:contact message)
        location (:location message)
        reply-to (:reply_to_message message)
        successful_payment (:successful_payment message)]
    (cond
      (= text "/start") (d/dispatch! ctx [:c/start])
      location (d/dispatch! ctx [:c/location])
      successful_payment (d/dispatch! ctx [:c/successful-payment])
      contact (d/dispatch! ctx [:c/contact])
      reply-to (d/dispatch! ctx [:c/reply])
      text (d/dispatch! ctx [:c/text]))))


(defn client-update-handler!
  [update]
  (let [message (:message update)
        inline-query (:inline_query update)
        callback-query (:callback_query update)
        pre-checkout-query (:pre_checkout_query update)
        ctx (build-ctx! update)
        blocked? (:is_blocked (:user ctx))]
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

;(build-ctx! {:update_id 220544587, :message {:message_id 10499, :from {:id 79225668, :is_bot false, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :language_code "ru"}, :chat {:id 79225668, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :type "private"}, :date 1581846213, :text "/start", :entities [{:offset 0, :length 6, :type "bot_command"}]}})
