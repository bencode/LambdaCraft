#!/bin/bash
set -e

SITE_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SITE_DIR/../.." && pwd)"
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

echo "Building Astro site..."
cd "$SITE_DIR"
npm run build

cd "$REPO_ROOT"
git checkout -B gh-pages

mkdir -p docs

echo "Copying build output to docs/..."
rm -rf docs/*
cp -r "$SITE_DIR/dist/"* docs/

echo "Creating .nojekyll file..."
touch docs/.nojekyll

echo "Creating CNAME file..."
echo "qijun.io" > docs/CNAME

git add -f docs/
git commit -m "Deploy site - $(date '+%Y-%m-%d %H:%M:%S')"
git push origin gh-pages --force

rm -rf docs
git checkout "$CURRENT_BRANCH"

echo "Deployed successfully!"
