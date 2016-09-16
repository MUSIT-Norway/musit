#!/bin/bash

export EVOLUTION_ENABLED=false

export SLICK_DRIVER=slick.driver.PostgresDriver$
export SLICK_DB_DRIVER=org.postgresql.Driver
export SLICK_DB_URL=jdbc:postgresql://db/postgres
export SLICK_DB_USER=postgres
export SLICK_DB_PASSWORD=postgres

export MILJO=dev

STARTDIR=$(pwd)

cd ../..
if [ ! -L ./frontend ]; then
       	ln -s ../musit-frontend frontend
fi

LOCAL=$(git rev-parse @)
REMOTE=$(git rev-parse @{u})
BASE=$(git merge-base @ @{u})
if [ $LOCAL = $REMOTE ]; then
    echo "Up-to-date"
elif [ $LOCAL = $BASE ]; then
    echo "Need to pull"
    git pull
    sbt clean docker:publishLocal
else
    echo "Everything is ok"
fi

cd frontend
git pull

cd ${STARTDIR}

docker-compose up -d --build --remove-orphans
