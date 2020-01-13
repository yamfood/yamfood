(ns yamfood.telegram.events
  (:require
    [yamfood.telegram.handlers.client.start]
    [yamfood.telegram.handlers.client.order]
    [yamfood.telegram.handlers.client.reply]
    [yamfood.telegram.handlers.client.basket]
    [yamfood.telegram.handlers.client.inline]
    [yamfood.telegram.handlers.client.product]
    [yamfood.telegram.handlers.client.callback]

    [yamfood.telegram.handlers.rider.order]
    [yamfood.telegram.handlers.rider.inline]
    [yamfood.telegram.handlers.rider.menu]
    [yamfood.telegram.handlers.rider.callback]))
