(ns yamfood.tasks.core
  (:require
    [overtone.at-at :as at]
    [yamfood.tasks.announcements :as a]))


(def pool (at/mk-pool))
;(at/stop-and-reset-pool! pool)


(defn run-tasks! []
  (at/every 5000 #(a/announcements-daemon!) pool))
