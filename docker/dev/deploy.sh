#!/bin/bash

echo "################################################################################"
echo "## IMPORTANT!"
echo "## This script is ONLY meant for DEVELOPMENT, and NOT for PRODUCTION."
echo "## If you are seeing this message in PRODUCTION, something has gone terribly wrong!"
echo "################################################################################"

export EVOLUTION_ENABLED=false
export APPLICATION_SECRET=dummyAppSecret
export SLICK_DRIVER=slick.driver.PostgresDriver$
export SLICK_DB_DRIVER=org.postgresql.Driver
export SLICK_DB_URL=jdbc:postgresql://db/postgres
export SLICK_DB_USER=postgres
export SLICK_DB_PASSWORD=postgres

STARTDIR=$(pwd)

echo "MUSARK: docker-compose stop ." && docker-compose stop > /dev/null
echo "MUSARK: docker-compose rm ." && docker-compose rm -f > /dev/null

cd ../..

echo "MUSARK: sbt clean docker:publishLocal ." && sbt clean docker:publishLocal > /dev/null

cd ${STARTDIR}

echo "MUSARK: docker-compose up ." && docker-compose up -d --build --force-recreate --remove-orphans > /dev/null
