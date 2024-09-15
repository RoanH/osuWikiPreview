# Updating
Updating to a new osu! version is fairly similar to the setup from scratch. However, it is very likely that a lot of steps here are redundant.

1. Pull the latest osu-web image and redis image (double check the current redis/elasticsearch images being used on the docker hub page)
1. For each instance
  1. Stop all containers: `docker stop osu-web-1 osu-web-redis-1 osu-web-elasticsearch-1`
  1. Remove all containers: `docker rm osu-web-1 osu-web-redis-1 osu-web-elasticsearch-1`
  1. Delete all 6 MySQL schemas (`osu1` + `all osu_*`)
  1. Follow the [Setup](Setup.md) commands to create a new redis container.
  1. Follow the [Setup](Setup.md) commands to create a new elasticsearch container.
  1. Run the preparation commands in the [Setup](Setup.md) file for osu! web.
  1. Start a new osu! web container with the [Setup](Setup.md) file.
  1. Finally, on Discord, switch at least once to a branch that has a newspost that changes to force a resync of all news.
