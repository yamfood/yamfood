(ns yamfood.utils
  (:require [clojure.edn :as edn])
  (:import (java.util UUID)))


(defn uuid [] (str (UUID/randomUUID)))


(defn str->int [s]
  (edn/read-string s))


(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))


