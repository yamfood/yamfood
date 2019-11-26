(ns yamfood.telegram.events
  (:require [yamfood.telegram.dispatcher :as d]
            [yamfood.telegram.handlers.start :as start]
            [yamfood.telegram.handlers.text :as text]
            [yamfood.telegram.handlers.callback :as callback]
            [yamfood.telegram.handlers.bucket :as bucket]
            [yamfood.telegram.handlers.inline :as inline]))


(d/register-event-handler!
  :inline
  inline/handle-inline-query)


(d/register-event-handler!
  :callback
  callback/handle-callback)


(d/register-event-handler!
  :products-done
  inline/return-products-to-inline-query)


(d/register-event-handler!
  :product-done
  text/react-to-text)


(d/register-event-handler!
  :update-markup
  bucket/update-markup)


(d/register-event-handler!
  :start
  start/handle-start)


(d/register-event-handler!
  :contact
  start/handle-contact)


(d/register-event-handler!
  :text
  text/handle-text)







