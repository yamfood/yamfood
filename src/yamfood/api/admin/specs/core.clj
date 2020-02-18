(ns yamfood.api.admin.specs.core
  (:require
    [clojure.spec.alpha :as s]))


(defn digits [n]
  (->> n str (map (comp read-string str))))


(defn digits-count?
  [c]
  (fn [num]
    (= c (count (digits num)))))


(s/def ::phone (s/and int? (digits-count? 12)))
