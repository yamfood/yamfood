(ns yamfood.tasks.core
  (:require
    [mount.core :as mount]
    [overtone.at-at :as at]
    [yamfood.tasks.sms :as sms]
    [yamfood.tasks.announcements :as a]
    [yamfood.tasks.iiko :as i]))

(mount/defstate pool
  :start (at/mk-pool)
  :stop (at/stop-and-reset-pool! pool))


(mount/defstate anouncements
  :start (at/every 5000 #(a/announcements-daemon!) pool)
  :stop (at/stop anouncements))


(mount/defstate sms
  :start (at/every 5000 #(sms/sms-daemon!) pool)
  :stop (at/stop sms))

#_(mount/stop #'sms #'anouncements)


(mount/defstate disabled-products
  :start (at/every (* 3600 1000) #(i/disabled-products-daemon!) pool)
  :stop (at/stop disabled-products))
