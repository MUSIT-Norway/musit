#!/bin/bash

# ------------------------------------------------------------------------
# Include secrets that can't be in this script
# ------------------------------------------------------------------------
source secrets.sh

echo "################################################################################"
echo "## IMPORTANT!"
echo "## This script is ONLY meant for DEVELOPMENT, and NOT for PRODUCTION."
echo "## If you are seeing this message in PRODUCTION, something has gone terribly wrong!"
echo "################################################################################"

# ------------------------------------------------------------------------
# Slick MUSIT DB configuration
# ------------------------------------------------------------------------
export EVOLUTION_ENABLED=false
export APPLICATION_SECRET=dummyAppSecret
export SLICK_DB_URL=jdbc:oracle:thin:@db:1521:orcl
export SLICK_DB_USER=musit
export SLICK_DB_PASSWORD=musit

# ------------------------------------------------------------------------
# Document module specific configuration (including postgres)
# ------------------------------------------------------------------------
export SYMBIOTIC_POSTGRES_HOST=postgres_db
export SYMBIOTIC_POSTGRES_PORT=5432
export SYMBIOTIC_DB_USER=postgres
export SYMBIOTIC_DB_PASSWORD=postgres
export SYMBIOTIC_DB_NAME=postgres
export SYMBIOTIC_FS_ROOT_DIR="dman/files"

# ------------------------------------------------------------------------
# The application defaults to use the fake security module (no.uio.musit.security.fake.FakeModule)
# If you want to test with Dataporten, make sure to remove the comment on the below line(s)
# Also you will need to set up an application in Dataporten to get access to a
# Callback URL, Client ID and Client secret.
# ------------------------------------------------------------------------
export MUSIT_ENV="dev"
export MUSIT_BASE_URL="http://localhost"
export DOCKET_HOSTNAME=$(hostname)
export MUSIT_SECURITY_MODULE="no.uio.musit.security.dataporten.DataportenModule"
export CALLBACK_URL="http://musit-test:8888/api/auth/rest/authenticate"

export DATAPORTEN_CLIENT_ID=$CLIENT_ID
export DATAPORTEN_CLIENT_SECRET=$CLIENT_SECRET
export DATAPORTEN_SESSION_TIMEOUT="4 hours"

# ------------------------------------------------------------------------
# Start the deployment process...
# ------------------------------------------------------------------------

STARTDIR=$(pwd)

echo "MUSARK: docker-compose stop ." && docker-compose stop > /dev/null

cd ../..

echo "MUSARK: sbt clean docker:publishLocal ." && sbt clean docker:publishLocal > /dev/null

cd ${STARTDIR}

echo "MUSARK: docker-compose up ." && docker-compose up -d --build --remove-orphans > /dev/null
