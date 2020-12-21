(ns yamfood.telegram.core
  (:require
    [compojure.core :as c]
    [yamfood.telegram.events]
    [yamfood.telegram.effects]
    [environ.core :refer [env]]
    [yamfood.telegram.handlers.rider.core :as rider]
    [yamfood.telegram.handlers.client.core :as client]
    [yamfood.utils :as utils]))


(defn log-debug
  [request]
  (println (str "\n\n" (:body request) "\n\n")))


(defn client-handler!
  [request]
  (let [token (:token (:params request))]
    (log-debug request)
    (try
      (client/client-update-handler! token (:body request))
      (catch Exception e
        (utils/log-error request e)))
    {:body "OK"}))


(defn rider-handler!
  [request]
  (log-debug request)
  (try
    (rider/rider-update-handler! (:body request))
    (catch Exception e
      (utils/log-error request e)))
  {:body "OK"})


(c/defroutes
  telegram-routes
  (c/POST "/client/:token/" [] client-handler!)
  (c/POST "/rider" [] rider-handler!))


(comment
  (let [client-token ""
        webhook-url ""]
    (morse.api/set-webhook client-token (str webhook-url (format "/updates/client/%s/" client-token)))
    (morse.api/set-webhook (env :rider-bot-token) (str webhook-url "/updates/rider"))))

