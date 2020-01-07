(ns yamfood.telegram.core
  (:require
    [yamfood.telegram.events]
    [yamfood.telegram.effects]
    [environ.core :refer [env]]
    [yamfood.telegram.handlers.client.core :as client]
    [yamfood.telegram.handlers.rider.core :as rider]
    [compojure.core :as c]))


(defn log-error
  [request e]
  (println
    (format
      "\n\n %s \n\n"
      {:update (:body request) :error e})))


(defn log-debug
  [request]
  (println (str "\n\n" (:body request) "\n\n")))


(defn client-handler!
  [request]
  (log-debug request)
  (try
    (client/client-update-handler! (:body request))
    (catch Exception e
      (log-error request e)))
  {:body "OK"})


(defn rider-handler!
  [request]
  (log-debug request)
  (try
    (rider/rider-update-handler! (:body request))
    (catch Exception e
      (log-error request e)))
  {:body "OK"})


(c/defroutes
  telegram-routes
  (c/POST "/client" [] client-handler!)
  (c/POST "/rider" [] rider-handler!))


;(morse.api/set-webhook (env :bot-token) "https://2dad0ff7.ngrok.io/updates/client")
;(morse.api/set-webhook (env :rider-bot-token) "https://2dad0ff7.ngrok.io/updates/rider")