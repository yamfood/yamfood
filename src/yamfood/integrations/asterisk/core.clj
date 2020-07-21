(ns yamfood.integrations.asterisk.core
  (:require
    [mount.core :as mount]
    [clj-asterisk.events :as events]
    [clj-asterisk.manager :as manager]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.api.admin.handlers.calls :as calls]))


(defmethod events/handle-event "AgentCalled"
  [event _]
  (let [phone (u/parse-int (str "998" (:CallerIDNum event)))
        number (:MemberName event)
        destination (:DestCallerIDNum event)]
    (calls/new-call! phone number destination)))


(defn get-context!
  []
  (manager/with-config
    {:name "185.230.205.114" :port 5038}
    (manager/login "webmanager" "world" :with-events)))


(defn ping!
  [context]
  (manager/with-connection
    context
    (not (= (:type (manager/action :PING))
            :clj-asterisk.internal.core/timeout))))


(defn connect-and-listen-events!
  []
  (loop [context (get-context!)]
    (Thread/sleep 5000)
    (if (ping! context)
      (recur context)
      (recur (get-context!)))))


(mount/defstate
  asterisk-ami
  :start connect-and-listen-events!
  :stop (println "stop"))


#_(mount/stop #'sms #'asterisk-ami)
