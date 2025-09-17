#!/bin/sh
set -eu

LOCK="/tmp/deploy.lock"
if ! mkdir "$LOCK" 2>/dev/null; then
  echo "[deploy] another deployment running"; exit 0
fi
trap 'rmdir "$LOCK"' EXIT

cd /stack/deploy

echo "[deploy] docker compose pull && up -d (api)"
docker compose pull api
docker compose up -d --force-recreate api
echo "[deploy] done"
