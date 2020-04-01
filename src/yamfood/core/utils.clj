(ns yamfood.core.utils)


(defn keywordize
  [data]
  (into
    {}
    (for [[k v] data]
      [(keyword k) (if (map? v) (keywordize v) v)])))


(defn keywordize-field
  ([map]
   (keywordize-field map :payload))
  ([map field]
   (let [payload (get map field)]
     (assoc map field (keywordize payload)))))

