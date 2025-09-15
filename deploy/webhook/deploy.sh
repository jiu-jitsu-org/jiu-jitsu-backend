#!/usr/bin/env bash
set -euo pipefail
echo "[deploy] $(date) pulling images..."
docker compose -f deploy/docker-compose.yml pull
echo "[deploy] restarting..."
docker compose -f deploy/docker-compose.yml up -d
echo "[deploy] prune..."
docker image prune -f
echo "[deploy] done."
