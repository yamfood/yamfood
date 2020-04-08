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


(defn update-if-exists
  [m k f]
  (if (contains? m k)
    (assoc m k (f (get m k)))
    m))
