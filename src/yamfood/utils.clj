(ns yamfood.utils
  (:require [clojure.edn :as edn])
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
