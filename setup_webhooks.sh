#!/bin/bash

webhook_url=$APP_NAME.herokuapp.com/updates

echo $webhook_url

curl "https://api.telegram.org/bot$BOT_TOKEN/setWebhook?url=$webhook_url/client"
curl "https://api.telegram.org/bot$RIDER_BOT_TOKEN/setWebhook?url=$webhook_url/rider"
