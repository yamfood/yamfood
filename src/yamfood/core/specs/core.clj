(ns yamfood.core.specs.core
  (:require
    [clojure.spec.alpha :as s]))


(defn digits [n]
  (->> n str (map (comp read-string str))))


(defn digits-count?
  [c]
  (fn [num]
    (= c (count (digits num)))))


(defn timestamp?
  [str]
  ; TODO: Implement regexp
  (and (string? str)
       true))


(s/def ::phone (s/and number? (digits-count? 12)))
