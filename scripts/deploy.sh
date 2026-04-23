#!/usr/bin/env bash
set -euo pipefail

SERVER="${DEPLOY_SERVER:-root@REDACTED}"
REMOTE_DIR="${DEPLOY_DIR:-REDACTED}"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo "==> Sync source to $SERVER:$REMOTE_DIR"
ssh "$SERVER" "mkdir -p $REMOTE_DIR"
rsync -az --delete \
  --exclude='.git' \
  --exclude='node_modules' \
  --exclude='**/node_modules' \
  --exclude='dist' \
  --exclude='**/dist' \
  --exclude='.astro' \
  --exclude='**/.astro' \
  --exclude='*.log' \
  --exclude='.env' \
  --exclude='.env.*' \
  --exclude='.DS_Store' \
  ./ "$SERVER:$REMOTE_DIR/"

echo "==> Build & restart container on server"
ssh "$SERVER" "cd $REMOTE_DIR && docker compose up -d --build"

echo "==> Done."
echo "    https://qijun.io"
echo "    https://www.qijun.io"
echo "    (host port: http://REDACTED:8081)"
