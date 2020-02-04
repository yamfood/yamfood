(ns yamfood.utils
  (:import (java.util UUID)))


(defn uuid [] (str (UUID/randomUUID)))

