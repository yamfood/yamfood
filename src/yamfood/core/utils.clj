(ns yamfood.core.utils)


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

