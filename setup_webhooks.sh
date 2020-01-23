#!/bin/bash

webhook_url=$(heroku info -s | grep web_url | cut -d= -f2)updates
curl "https://api.telegram.org/bot$BOT_TOKEN/setWebhook?url=$webhook_url/client"
curl "https://api.telegram.org/bot$RIDER_BOT_TOKEN/setWebhook?url=$webhook_url/rider"
