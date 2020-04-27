(ns yamfood.core.sms.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [environ.core :refer [env]]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [clojure.set :refer [difference]]))


(def all-sms-query
  {:select [:sms.id
            :sms.text
            :sms.phone
            :sms.created_at
            :sms.is_sent
            :sms.error]
   :from   [:sms]})


(defn all-sms!
  ([]
   (all-sms! 0 100))
  ([offset limit]
   (->> (-> all-sms-query
            (assoc :offset offset)
            (assoc :limit limit)
            (hs/format))
        (jdbc/query db/db))))


(defn sms-to-send!
  [limit]
  (->> (-> all-sms-query
           (assoc :limit limit)
           (hh/merge-where [:and
                            [:< :sms.created_at :current_timestamp]
                            [:= :sms.is_sent false]])
           (hs/format))
       (jdbc/query db/db)))


(defn update!
  [sms-id row]
  (jdbc/update!
    db/db
    "sms"
    row
    ["id = ?" sms-id]))
