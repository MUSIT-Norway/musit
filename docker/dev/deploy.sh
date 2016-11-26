#!/bin/bash

echo "################################################################################"
echo "## IMPORTANT!"
echo "## This script is ONLY meant for DEVELOPMENT, and NOT for PRODUCTION."
echo "## If you are seeing this message in PRODUCTION, something has gone terribly wrong!"
echo "################################################################################"

# ------------------------------------------------------------------------
# Slick Database configuration
# ------------------------------------------------------------------------
export EVOLUTION_ENABLED=false
export APPLICATION_SECRET=dummyAppSecret
export SLICK_DRIVER=com.typesafe.slick.driver.oracle.OracleDriver$
export SLICK_DB_DRIVER=oracle.jdbc.OracleDriver
export SLICK_DB_URL=jdbc:oracle:thin:@db:1521:orcl
export SLICK_DB_USER=musit
export SLICK_DB_PASSWORD=musit

# ------------------------------------------------------------------------
# The application defaults to use the fake security module (no.uio.musit.security.fake.FakeModule)
# If you want to test with Dataporten, make sure to remove the comment on the below line(s)
# Also you will need to set up an application in Dataporten to get access to a
# Callback URL, Client ID and Client secret.
# ------------------------------------------------------------------------
# export MUSIT_SECURITY_MODULE="no.uio.musit.security.dataporten.DataportenModule"
# export CALLBACK_URL=""
# export CLIENT_ID=""
# export CLIENT_SECRET=""
# export DATAPORTEN_CLIENT_ID=$CLIENT_ID
# export DATAPORTEN_CLIENT_SECRET=$CLIENT_SECRET

# ------------------------------------------------------------------------
# Start the deployment process...
# ------------------------------------------------------------------------

STARTDIR=$(pwd)

echo "MUSARK: docker-compose stop ." && docker-compose stop > /dev/null

cd ../..

echo "MUSARK: sbt clean docker:publishLocal ." && sbt clean docker:publishLocal > /dev/null

cd ${STARTDIR}

echo "MUSARK: docker-compose up ." && docker-compose up -d --build --force-recreate --remove-orphans > /dev/null
