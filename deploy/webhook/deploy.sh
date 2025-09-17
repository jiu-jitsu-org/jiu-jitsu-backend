#!/bin/sh
set -eu

LOCK="/tmp/deploy.lock"
if ! mkdir "$LOCK" 2>/dev/null; then
  echo "[deploy] another deployment running"; exit 0
fi
trap 'rmdir "$LOCK"' EXIT

WORKDIR="/stack/deploy"
COMPOSE="docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $WORKDIR:$WORKDIR -w $WORKDIR \
  docker/compose:2.29.2"

echo "[deploy] pull & recreate 'api' in $WORKDIR"
$COMPOSE pull api
$COMPOSE up -d --force-recreate api
echo "[deploy] done"
