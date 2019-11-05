(ns yamfood.telegram.events
  (:require [yamfood.telegram.dispatcher :as d]
            [yamfood.telegram.handlers.start :as start]
            [yamfood.telegram.handlers.text :as text]
            [yamfood.telegram.handlers.inline :as inline]))


(d/register-event-handler!
  :inline
  inline/handle-inline-query)


(d/register-event-handler!
  :start
  start/handle-start)


(d/register-event-handler!
  :contact
  start/handle-contact)


(d/register-event-handler!
  :text
  text/handle-text)







