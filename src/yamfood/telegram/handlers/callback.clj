(ns yamfood.telegram.handlers.callback
  (:require [yamfood.telegram.handlers.bucket :as bucket]
            [yamfood.telegram.handlers.utils :as u]))


(defn handle-callback
  [ctx update]
  (let [query (:callback_query update)
        action (u/get-callback-action (:data query))]
    (cond
      (= action "want") (bucket/handle-want ctx update)
      (= action "+") (bucket/handle-inc ctx update)
      (= action "-") (bucket/handle-dec ctx update))))


