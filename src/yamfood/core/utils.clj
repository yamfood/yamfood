(ns yamfood.core.utils
  (:require [clojure.string :as str]))


(defn- keywordize
  [data]
  (if (map? data)
    (into {}
          (for [[key val] data]
            [(keyword key) (cond
                             (map? val) (keywordize val)
                             (vector? val) (map keywordize val)
                             :else val)]))
    data))


(defn keywordize-field
  ([map]
   (keywordize-field map :payload))
  ([map field]
   (let [payload (get map field)]
     (assoc map field (keywordize payload)))))


(defn group-by-prefix [m prefix]
  (->> m
       (map (fn [[k v]]
              (let [k-split (str/split (name k) #"_")]
                (if (= (first k-split) (name prefix))
                  {(keyword (first k-split)) {(keyword (str/join "_" (rest k-split))) v}}
                  {k v}))))
       (apply merge-with merge)))
