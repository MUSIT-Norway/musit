#!/bin/bash

export EVOLUTION_ENABLED=false

export SLICK_DRIVER=slick.driver.PostgresDriver$
export SLICK_DB_DRIVER=org.postgresql.Driver
export SLICK_DB_URL=jdbc:postgresql://db/postgres
export SLICK_DB_USER=postgres
export SLICK_DB_PASSWORD=postgres

export MILJO=dev

STARTDIR=$(pwd)

echo "MUSARK: docker-compose stop ." && docker-compose stop > /dev/null
echo "MUSARK: docker-compose rm ." && docker-compose rm -f > /dev/null

cd ../..
if [ ! -L ./frontend ]; then
       	ln -s ../musit-frontend frontend
fi

echo "MUSARK: git pull backend ." && git pull > /dev/null
echo "MUSARK: sbt clean docker:publishLocal ." && sbt clean docker:publishLocal > /dev/null

cd frontend
echo "MUSARK: git pull frontend ." && git pull > /dev/null

cd ${STARTDIR}

echo "MUSARK: docker-compose build ." && docker-compose build --no-cache > /dev/null
echo "MUSARK: docker-compose up ." && docker-compose up -d --force-recreate --remove-orphans > /dev/null
