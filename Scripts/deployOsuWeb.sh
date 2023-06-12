docker network create osu-web-1

docker run -d --name osu-web-mysql -e MYSQL_ALLOW_EMPTY_PASSWORD=yes --network osu-web-1 mysql/mysql-server:8.0
while ! (docker exec osu-web-mysql mysql); do echo "Waiting for MySQL to be ready..."; sleep 3; done && curl https://raw.githubusercontent.com/ppy/osu-web/master/docker/development/db_user.sql | docker exec -i osu-web-mysql mysql

docker run -d --name osu-web-redis redis:latest

docker run -d --name osu-web-elasticsearch -e action.auto_create_index=false -e discovery.type=single-node -e ES_JAVA_OPTS="-Xms512m -Xmx512m" --network osu-web-1 docker.elastic.co/elasticsearch/elasticsearch:7.17.6

docker run --name passport_keys pppy/osu-web:latest artisan passport:keys
docker cp passport_keys:/app/storage/oauth-public.key .
docker cp passport_keys:/app/storage/oauth-private.key .
docker rm passport_keys

# this part is totally manual...

docker run --rm -it --network osu-web-1 --env-file .env pppy/osu-web:latest artisan db:create
docker run --rm -it --network osu-web-1 --env-file .env pppy/osu-web:latest artisan migrate
docker run --rm -it --network osu-web-1 --env-file .env pppy/osu-web:latest artisan es:index-documents
docker run --rm -it --network osu-web-1 --env-file .env pppy/osu-web:latest artisan es:create-search-blacklist
docker run --rm -it --network osu-web-1 --env-file .env pppy/osu-web:latest artisan es:index-wiki --create-only

#run (-d)
docker run --name osu-web-octane-deploy --network osu-web-1 --env-file .env -p 9591:8000 pppy/osu-web:latest octane