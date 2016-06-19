docker run -it --rm --name musit-backing-docker -v "$PWD":/usr/src/app -w /usr/src/app node:4 rm -rf node_modules && npm install && npm start
