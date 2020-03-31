(ns yamfood.telegram.helpers.push
  (:require
    [morse.api :as t]
    [environ.core :refer [env]]
    [yamfood.core.clients.core :as c]
    [honeysql.core :as hs]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [yamfood.utils :as u]
    [clojure.data.json :as json]))


(def token (env :bot-token))


(defn send-push!
  [tid image text]
  (t/send-photo
    token
    tid
    {:caption    text
     :parse_mode "markdown"}
    image))

(def photo "https://s3-eu-west-1.amazonaws.com/bucketeer-a6f8aeba-d25c-4758-8db5-b90b9d74d72d/products/skidka.jpg")
(def text "Получите скидку 15 000 сум на *первый заказ* по промокоду ПРИВЕТ\n\nДостаточно ввести промокод в комментарий к заказу \uD83D\uDCAC\n\nПриятного аппетита! \n\n_Скидка действует до 1 апреля (включительно)_")


(defn send-batch-push!
  [client photo text]
  (do
    (let [tid (:tid client)
          payload (:payload client)]
      (try
        (Thread/sleep 100)
        (send-push! tid photo text)
        (println (str "Sent " tid))
        (catch Exception e
          (let [body (:body (:data (Throwable->map e)))]
            (println (str tid " " body)))))
      (c/update-by-tid!
        tid
        {:payload (assoc payload :push true)}))))


(defn get-clients-for-not-push!
  []
  (->> (-> {:select [:clients.tid]
            :from   [:clients]
            :join   [:orders [:= :orders.client_id :clients.id]]}
           (hs/format))
       (jdbc/query db/db)
       (map :tid)))


;(def clients (map #(select-keys % [:tid :payload]) (c/clients-list! 0 10000 [:and (hs/raw "payload->>'push' is null")])))
;(first clients)
;
;(def not-send (get-clients-for-not-push!))
;
;(def clients-for-push (filter #(not (u/in? not-send (:tid %))) clients))
;
;(first clients-for-push)
;
;(pmap #(send-batch-push! % photo text) clients-for-push)
