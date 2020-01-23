#!/bin/bash

tokens=($BOT_TOKEN $RIDER_TOKEN)
webhook_url=$(heroku info -s | grep web_url | cut -d= -f2)

for token in ${tokens[@]}
do
    echo "https://api.telegram.org/bot$token/setWebhook?url=$webhook_url"
done