cp ../fake_security.json .
docker stop musit-frontend
docker rm musit-frontend
docker build -t musit/frontend .
docker run --name musit-frontend -v $(pwd)/src:/usr/src/app/src -v $(pwd)/public:/usr/src/app/public -d -p 8000:8000 musit/frontend
