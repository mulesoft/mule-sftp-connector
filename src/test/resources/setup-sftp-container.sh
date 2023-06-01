#!/bin/bash

CONTAINER_ID=$(docker ps -qf "name=openssh")
docker exec $CONTAINER_ID mkdir /config/privateDirectory
docker exec $CONTAINER_ID touch /config/privateDirectory/file-to-renamed.txt
docker exec $CONTAINER_ID sudo chown -R root:root /config/privateDirectory