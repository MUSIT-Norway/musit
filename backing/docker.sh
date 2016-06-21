docker run -it --rm --name musit-backing -v "$PWD":/usr/src/app -w /usr/src/app node:5 rm -rf node_modules && npm install && npm start
