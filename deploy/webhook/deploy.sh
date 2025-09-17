#!/bin/sh
set -eu

LOCK="/tmp/deploy.lock"
if ! mkdir "$LOCK" 2>/dev/null; then
  echo "[deploy] another deployment running"; exit 0
fi
trap 'rmdir "$LOCK"' EXIT

# ⬇️ 매 배포마다 조용히 로그인(이미 로그인 상태여도 무시)
echo "$GHCR_PAT" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin >/dev/null 2>&1 || true

cd /stack/deploy
echo "[deploy] docker compose pull && up -d (api)"
docker compose pull api
docker compose up -d --force-recreate api
echo "[deploy] done"
