# Setup
The setup is based on [pppy/osu-web](https://hub.docker.com/r/pppy/osu-web).

First a docker network is required:
```sh
docker network create osu-web-1
```

## MySQL
Either follow the containerized setup procedure for MySQL on the official Docker HUB page _or_ already have a MySQL instance running somewhere. For convenience the container setup script is:

```sh
docker run -d --name osu-web-mysql-1 -e MYSQL_ALLOW_EMPTY_PASSWORD=yes --network osu-web-1 mysql/mysql-server:8.0
while ! (docker exec osu-web-mysql-1 mysql); do echo "Waiting for MySQL to be ready..."; sleep 3; done && curl https://raw.githubusercontent.com/ppy/osu-web/master/docker/development/db_user.sql | docker exec -i osu-web-mysql-1 mysql
```

## Redis
Relatively straightforward just the latest version:

```sh
docker run -d --name osu-web-redis-1 --network osu-web-1 redis:latest
```

## Elastic Search
Has to be version 7.17.6 as of right now:

```sh
docker run -d --name osu-web-elasticsearch-1 -e action.auto_create_index=false -e discovery.type=single-node -e ES_JAVA_OPTS="-Xms512m -Xmx512m" --network osu-web-1 docker.elastic.co/elasticsearch/elasticsearch:7.17.6
```

## Configure .env
Follow the instructions as present on the [Docker HUB](https://hub.docker.com/r/pppy/osu-web) page, some notes:
- Make sure to use the correct container names.
- If using an external MySQL instance update the `DB_*` properties.
- Set the GitHub token.
- Disable registration.
- Configure the `WIKI_*` properties.

## Prepare osu! web
Need to run some database and index initialisation.

```sh
docker run --rm -it --network osu-web-1 --env-file osu1.env pppy/osu-web:latest artisan db:create
docker run --rm -it --network osu-web-1 --env-file osu1.env pppy/osu-web:latest artisan migrate
docker run --rm -it --network osu-web-1 --env-file osu1.env pppy/osu-web:latest artisan es:index-documents
docker run --rm -it --network osu-web-1 --env-file osu1.env pppy/osu-web:latest artisan es:create-search-blacklist
docker run --rm -it --network osu-web-1 --env-file osu1.env pppy/osu-web:latest artisan es:index-wiki --create-only
```

## Run osu! web
The run the latest version of osu! web:

```sh
docker run -d --name osu-web-1 --network osu-web-1 --env-file osu1.env -p 9591:8000 pppy/osu-web:latest octane
```
