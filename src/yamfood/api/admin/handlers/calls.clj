(ns yamfood.api.admin.handlers.calls
  (:require
    [aleph.http :as http]
    [compojure.core :as c]
    [manifold.stream :as stream]
    [clojure.data.json :as json]
    [yamfood.core.admin.core :as a]
    [yamfood.core.bots.core :as bots]
    [yamfood.core.clients.core :as clients]
    [yamfood.telegram.handlers.utils :as u]
    [clojure.string :as str]))


(defonce connected-admins (atom {}))


(def non-websocket-request
  {:status  400
   :headers {"content-type" "application/text"}
   :body    "Expected a websocket request."})


(defn message->clj
  [message]
  (json/read-str message :key-fn keyword))


(defn consumer!
  [socket message]
  (let [data (message->clj message)
        token (:token data)
        admin (a/admin-by-token! token)
        admin-number (:number admin)]
    (swap! connected-admins assoc (str admin-number) socket)))


(defn close-fn!
  [message]
  (fn []
    (let [data (message->clj message)
          token (:token data)
          admin (a/admin-by-token! token)
          admin-number (:number admin)]
      (swap! connected-admins dissoc (str admin-number)))))


(defn ws-handler
  [req]
  (if-let [socket (try
                    @(http/websocket-connection req)
                    (catch Exception e
                      nil))]
    (do
      (let [message @(stream/take! socket)]
        (consumer! socket message)
        (stream/on-closed
          socket
          (close-fn! message)))
      nil)
    non-websocket-request))


(defn new-call!
  [phone admin-number destination]
  (let [socket (get @connected-admins (str admin-number))
        bot-id (:id (bots/bot-by-destination! destination))
        client (clients/get-or-create-external-client! bot-id phone)
        data {:client_id (:id client) :phone (str phone)}]
    (when socket
      (stream/put! socket (json/write-str data)))))


(defn get-connections
  []
  (map #(hash-map (first %) (stream/description (second %))) @connected-admins))


(defn connected-admins-list
  [_]
  {:body (or (get-connections) [])})


(defn new-call-handler
  [request]
  (let [params (:params request)
        phone (get params "number")
        login (get params "login")
        caller-name (get params "callername")
        destination (first (str/split ":" caller-name))]
    (println (str "\n\n" params "\n\n"))
    (if (and phone login destination)
      (let [phone (u/parse-int phone)]
        (new-call! phone login destination)
        {:status 200 :body {:result "ok"}})
      {:status 400 :body {:error "Invalid input"}})))


(c/defroutes
  routes
  (c/GET "/new/" [] new-call-handler)
  (c/GET "/connected/" [] connected-admins-list))
