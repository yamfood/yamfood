(ns yamfood.telegram.helpers.push
  (:require
    [morse.api :as t]
    [environ.core :refer [env]]))


(defn send-push!
  [tid token image text]
  (t/send-photo
    token
    tid
    {:caption    text
     :parse_mode "markdown"}
    image))
