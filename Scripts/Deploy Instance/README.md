



Setup



reddis host
cache redis host
db host
es host
notification redis host

ports
inst 1 9591
9592
9593

docker run -d --name osu-web-1 --network osu-web-1 --env-file .env -p 9591:8000 pppy/osu-web:latest octane
docker run -d --name osu-web-2 --network osu-web-1 --env-file .env -p 9592:8000 pppy/osu-web:latest octane
docker run -d --name osu-web-3 --network osu-web-1 --env-file .env -p 9593:8000 pppy/osu-web:latest octane