(ns yamfood.core.announcements.core
  (:require
    [yamfood.utils :as u]
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [clj-time.coerce :as timec]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]))


(def announcement-statuses
  {:scheduled "scheduled"
   :sending   "sending"
   :sent      "sent"})


(def announcement-query
  {:select   [:announcements.id
              :announcements.bot_id
              [:bots.name :bot]
              :announcements.image_url
              :announcements.text
              :announcements.status
              :announcements.send_at]
   :from     [:announcements :bots]
   :where    [:and
              [:= :announcements.is_active true]
              [:= :announcements.bot_id :bots.id]]
   :order-by [[:announcements.send_at :desc]]})


(defn all-announcements!
  ([]
   (all-announcements! 0 100))
  ([offset limit]
   (->> (-> announcement-query
            (assoc :offset offset)
            (assoc :limit limit)
            (hs/format))
        (jdbc/query db/db))))


(defn all-announcements-count!
  []
  (->> (-> announcement-query
           (assoc :select [:%count.announcements.id])
           (dissoc :order-by)
           (hs/format))
       (jdbc/query db/db)
       (first)
       (:count)))


(defn announcements-to-send!
  []
  (->> (-> announcement-query
           (hh/merge-where [:and
                            [:< :announcements.send_at :current_timestamp]
                            [:= :announcements.status (:scheduled announcement-statuses)]])
           (hs/format))
       (jdbc/query db/db)))


(defn announcement-by-id!
  [id]
  (->> (-> announcement-query
           (hh/merge-where [:= :announcements.id id])
           (hs/format))
       (jdbc/query db/db)
       (first)))


(defn prepare-announcement-for-update
  [announcement]
  (-> announcement
      (u/update-if-exists :send_at timec/to-sql-time)))


(defn update!
  [announcement-id row]
  (let [row (prepare-announcement-for-update row)]
    (jdbc/update!
      db/db
      "announcements"
      row
      ["id = ?" announcement-id])))


(defn prepare-announcement-for-create
  [announcement]
  (-> (if (contains? announcement :status)
        announcement
        (assoc announcement :status (:scheduled announcement-statuses)))
      (u/update-if-exists :send_at timec/to-sql-time)))


(defn create!
  [announcement]
  (let [announcement (prepare-announcement-for-create announcement)]
    (jdbc/insert!
      db/db
      "announcements"
      announcement)))


(defn delete!
  [announcement-id]
  (jdbc/update!
    db/db
    "announcements"
    {:is_active false}
    ["id = ?" announcement-id]))
