(ns yamfood.telegram.handlers.rider.core
  (:require
    [environ.core :refer [env]]
    [yamfood.telegram.dispatcher :as d]))


(defn build-ctx!
  [update]
  {:token  (env :rider-bot-token)
   :update update})


(defn process-message
  [ctx update]
  (let [message (:message update)
        text (:text message)]
    (cond
      text (d/dispatch! ctx [:r/text]))))


(defn rider-update-handler!
  [update]
  (let [message (:message update)
        ctx (build-ctx! update)]
    (if message
      (process-message ctx update))))



(rider-update-handler!
  (:text (:message {:update_id 224712354, :message {:message_id 3, :from {:id 79225668, :is_bot false, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :language_code "ru"}, :chat {:id 79225668, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :type "private"}, :date 1578412350, :text "/start", :entities [{:offset 0, :length 6, :type "bot_command"}]}})))


