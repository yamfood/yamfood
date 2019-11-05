(ns yamfood.telegram.core
  (:require [yamfood.core.users.core :as users]
            [yamfood.telegram.dispatcher :as d]
            [yamfood.telegram.events]
            [environ.core :refer [env]]))


(def token (env :bot-token))


(defn log
  [message]
  (println (str "\n\n\n ### \n" message "\n\n\n")))


(defn get-tid-from-update ; Make it work with all updates!
  [update]
  (let [message (:message update)]
    (:id (:from message))))


(defn build-ctx
  [update]
  {:token token
   :user  (users/get-user-by-tid! (get-tid-from-update update))})


(defn process-message
  [ctx update]
  (let [message (:message update)
        text (:text message)
        contact (:contact message)]
    (cond
      (= text "/start") (d/dispatch [:start ctx update])
      contact (d/dispatch [:contact ctx update])
      text (d/dispatch [:text ctx message]))))


(defn process-updates
  [request]
  (log (:body request))
  (let [update (:body request)
        message (:message update)
        inline-query (:inline_query update)
        ctx (build-ctx update)]
    (if message
      (process-message ctx update))
    (if inline-query
      (d/dispatch [:inline ctx update])))
  {:body "OK"})

;(process-updates {:body {:update_id 435322249, :inline_query {:id "340271656996400178", :from {:id 79225668, :is_bot false, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :language_code "ru"}, :query "", :offset ""}}})
;(t/set-webhook token "https://c00eb9e9.ngrok.io/updates")

