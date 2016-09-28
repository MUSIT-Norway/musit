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

echo "MUSARK: sbt clean docker:publishLocal ." && sbt clean docker:publishLocal > /dev/null

cd ${STARTDIR}

echo "MUSARK: docker-compose up ." && docker-compose up -d --build --force-recreate --remove-orphans > /dev/null
