docker build -t musit/frontend .
docker run -v $(pwd)/src:/usr/src/app/src -v $(pwd)/public:/usr/src/app/public -d -p 8000:8000 musit/frontend
