#!/usr/bin/env bash

docker-compose down --rmi local --volumes

set -e
./gradlew clean assemble
docker-compose build webbaseimage
docker-compose up -d agent94 agent95
docker-compose up -d indypool
docker-compose up agentInitiator

#./gradlew test

docker-compose up -d webapp
