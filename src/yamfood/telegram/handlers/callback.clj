(ns yamfood.telegram.handlers.callback
  (:require [yamfood.telegram.handlers.bucket :as bucket]
            [yamfood.telegram.handlers.utils :as u]))

; TODO: Rewrite this to as dispatcher event handlers
(defn handle-callback
  [ctx update]
  (let [query (:callback_query update)
        action (u/get-callback-action (:data query))]
    (cond
      (= action "want") (bucket/handle-want ctx update)
      (= action "detail+") (bucket/handle-inc ctx update)
      (= action "detail-") (bucket/handle-dec ctx update)
      (= action "bucket") (bucket/handle-basket ctx update)
      (= action "bucket+") (bucket/handle-bucket-inc ctx update)
      (= action "bucket-") (bucket/handle-bucket-dec ctx update))))
