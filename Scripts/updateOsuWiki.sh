#!/bin/bash
cd /home/roan/wiki/osu-web
docker compose exec php /app/docker/development/entrypoint.sh artisan tinker --execute="OsuWiki::updateFromGithub(['before' => '$1','after' => '$2'])"
