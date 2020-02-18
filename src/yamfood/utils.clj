(ns yamfood.utils
  (:import (java.util UUID)))


(defn uuid [] (str (UUID/randomUUID)))


(defn parse-int [s]
  (let [r (re-find #"\d+" s)]
    (when r (Integer. r))))


(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))


