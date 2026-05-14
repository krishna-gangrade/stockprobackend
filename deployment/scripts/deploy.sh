#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/stockpro}"
BRANCH="${BRANCH:-main}"

cd "$APP_DIR"

if [ ! -d .git ]; then
  echo "Repository not found in $APP_DIR"
  exit 1
fi

git fetch origin
git checkout "$BRANCH"
git pull origin "$BRANCH"

if [ ! -f .env ]; then
  echo ".env is missing in $APP_DIR"
  exit 1
fi

docker compose pull || true
docker compose up -d --build --remove-orphans
docker image prune -f

echo "Deployment complete for branch: $BRANCH"
