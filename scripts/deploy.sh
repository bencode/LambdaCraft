#!/usr/bin/env bash
# Usage:
#   DEPLOY_SERVER=i DEPLOY_DIR=/root/repos/lambdacraft ./scripts/deploy.sh
# Recommended: define an SSH alias in ~/.ssh/config so the host/IP/user/key never
# appear on command lines, in shell history, or in this repo.

set -euo pipefail

: "${DEPLOY_SERVER:?DEPLOY_SERVER is required (e.g. user@host or an SSH alias from ~/.ssh/config)}"
: "${DEPLOY_DIR:?DEPLOY_DIR is required (absolute remote path, e.g. /root/repos/lambdacraft)}"

SERVER="$DEPLOY_SERVER"
REMOTE_DIR="$DEPLOY_DIR"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo "==> Sync source to remote:$REMOTE_DIR"
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
