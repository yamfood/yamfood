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
  (some #(= elm %) coll))


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
