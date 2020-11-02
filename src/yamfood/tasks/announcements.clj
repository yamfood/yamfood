(ns yamfood.tasks.announcements
  (:require
    [clojure.tools.logging :as log]
    [yamfood.core.clients.core :as c]
    [yamfood.core.bots.core :as bots]
    [yamfood.core.announcements.core :as a]
    [yamfood.telegram.helpers.push :as push]))


(defn send-announcement-to-client!
  [token client photo text]
  (do
    (let [tid (:tid client)]
      (try
        (Thread/sleep 100)
        (push/send-push! tid token photo text)
        (println (str "Sent " tid))
        (catch Exception e
          (let [body (:body (:data (Throwable->map e)))]
            (println (str tid " " body))))))))


(defn send-announcement!
  [announcement]
  (let [announcement-id (:id announcement)
        bot (bots/bot-by-id! (:bot_id announcement))
        photo (:image_url announcement)
        text (:text announcement)]
    (a/update! announcement-id {:status (:sending a/announcement-statuses)})
    (doall
      (pmap #(send-announcement-to-client! (:token bot) % photo text)
            (c/clients-tids-list! 0 1 [:= :clients.bot_id (:id bot)])))
    (a/update! announcement-id {:status (:sent a/announcement-statuses)})))


(defn announcements-daemon!
  []
  (try
    (log/info "Looking for new announcements...")
    (doall
      (->> (a/announcements-to-send!)
           (map send-announcement!)))
    (catch Exception e
      (println e))))
