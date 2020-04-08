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
  (and (string? str)
       (not (nil? (re-matches
                    #"^(?:[1-9]\d{3}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1\d|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[1-9]\d(?:0[48]|[2468][048]|[13579][26])|(?:[2468][048]|[13579][26])00)-02-29)T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d(?:Z|[+-][01]\d:[0-5]\d)$"
                    str)))))


(s/def ::phone (s/and number? (digits-count? 12)))
