#!/bin/bash
cd /home/roan/osu-web
docker compose exec php /app/docker/development/entrypoint.sh artisan tinker --execute="NewsPost::syncAll()"

