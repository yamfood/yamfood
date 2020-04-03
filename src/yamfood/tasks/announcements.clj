(ns yamfood.tasks.announcements
  (:require
    [yamfood.core.clients.core :as c]
    [yamfood.core.announcements.core :as a]
    [yamfood.telegram.helpers.push :as push]))


(defn send-announcement-to-client!
  [client photo text]
  (do
    (let [tid (:tid client)]
      (try
        (Thread/sleep 100)
        (push/send-push! tid photo text)
        (println (str "Sent " tid))
        (catch Exception e
          (let [body (:body (:data (Throwable->map e)))]
            (println (str tid " " body))))))))


(defn send-announcement!
  [announcement]
  (let [announcement-id (:id announcement)
        photo (:image_url announcement)
        text (:text announcement)]
    (a/update! announcement-id {:status (:sending a/announcement-statuses)})
    (doall
      (pmap #(send-announcement-to-client! % photo text)
            (map #(select-keys % [:tid]) (c/clients-list! 0 nil))))
    (a/update! announcement-id {:status (:sent a/announcement-statuses)})))


(defn announcements-daemon!
  []
  (try
    (println "Looking for new announcements...")
    (doall
      (->> (a/announcements-to-send!)
           (map send-announcement!)))
    (catch Exception e
      (println e))))