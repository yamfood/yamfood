(ns yamfood.utils
  (:require [clojure.edn :as edn]
            [clj-time.format :as timef]
            [clj-time.coerce :as timec])
  (:import (java.util UUID)
           (java.text SimpleDateFormat)))


(defn uuid [] (str (UUID/randomUUID)))


(defn str->int [s]
  (edn/read-string s))


(defn in?
  "true if coll contains elm"
  [coll elm]
  (or (some #(= elm %) coll)
      false))


(defn ->time
  [inst]
  (.format
    (SimpleDateFormat. "hh:mm")
    inst))


(defn time->sql
  [time]
  (->> time
       (timef/parse)
       (timec/to-sql-time)))


(defn timestamp->sql
  [timestamp]
  (->> timestamp
       (#(if (string? %)
           (timef/parse (timef/formatter "yyyy-MM-dd HH:mm") %)
           %))
       (timec/to-sql-date)))
