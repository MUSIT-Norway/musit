FROM node:latest

VOLUME ["/usr/src/app/src"]
VOLUME ["/usr/src/app/public"]
WORKDIR "/usr/src/app"
ADD package.json package.json
RUN npm install
ADD webpack.dev.config.js webpack.dev.config.js
ADD .babelrc .babelrc
ADD .eslintrc .eslintrc
ADD start.sh start.sh
RUN chmod +x start.sh

EXPOSE 8000

ENTRYPOINT ./start.sh
