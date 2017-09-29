#!/bin/bash

ARG=$1

CONTAINER_NAME="musit-postgres"

# Clean docker repo and folders
function clean {
  echo "Removing $CONTAINER_NAME container..."
  docker rm musit-postgres
}
# Stop docker
function stop {
  echo "Stopping $CONTAINER_NAME container..."
  docker stop $CONTAINER_NAME
  echo "$CONTAINER_NAME container stopped."
}
# Init and/or start docker containers
function start {
  PGSQL_EXISTS=$( docker ps --quiet --filter name=$CONTAINER_NAME )

  # Try to start a Postgres container
  if [[ -n "$PGSQL_EXISTS" ]]; then
    echo "Starting $CONTAINER_NAME..."
    docker start $CONTAINER_NAME
  else
    docker pull postgres:9.6
    docker run --name $CONTAINER_NAME -p 5432:5432 -e POSTGRES_PASSWORD=postgres -d postgres
  fi

  echo "$CONTAINER_NAME container started."
}

function reset {
  stop
  clean
  start
}

if [ "$ARG" == "start" ]; then
  start

elif [ "$ARG" == "stop" ]; then
  stop

elif [ "$ARG" == "clean" ]; then
  stop
  clean
elif [ "$ARG" == "reset" ]; then
  reset
else
  echo "USAGE: ./docker-postgres.sh [start|stop|clean|reset]"
fi