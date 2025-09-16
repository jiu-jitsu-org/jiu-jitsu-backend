#!/usr/bin/env bash
set -euo pipefail

LOCK="/tmp/deploy.lock"
if ! mkdir "$LOCK" 2>/dev/null; then
  echo "[deploy] another deployment running"; exit 0
fi
trap 'rmdir "$LOCK"' EXIT

echo "[deploy] git pull은 불필요 (이미지 배포 방식)"
echo "[deploy] docker compose pull && up -d"
docker compose pull
docker compose up -d
echo "[deploy] done"
