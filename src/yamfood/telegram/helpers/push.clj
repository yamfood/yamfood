(ns yamfood.telegram.helpers.push
  (:require
    [morse.api :as t]
    [environ.core :refer [env]]))


(def token (env :bot-token))


(defn send-push!
  [tid image text]
  (t/send-photo
    token
    tid
    {:caption    text
     :parse_mode "markdown"}
    image))
