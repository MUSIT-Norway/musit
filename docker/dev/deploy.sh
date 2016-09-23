#!/bin/bash

STARTDIR=$(pwd)

echo "MUSARK: docker-compose stop ." && docker-compose stop > /dev/null
echo "MUSARK: docker-compose rm ." && docker-compose rm -f > /dev/null

cd ../..

#echo "MUSARK: sbt clean docker:publishLocal ." && sbt clean docker:publishLocal > /dev/null

cd ${STARTDIR}

echo "MUSARK: docker-compose up ." && docker-compose up -d --build --force-recreate --remove-orphans > /dev/null
