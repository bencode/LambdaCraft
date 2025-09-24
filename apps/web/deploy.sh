#!/bin/bash

echo "Building static site..."
clojure -X:build

cd ../..
git checkout -B release

mkdir -p docs

echo "Copying files to docs directory..."
cp -r apps/web/public/* docs/

echo "Creating .nojekyll file..."
touch docs/.nojekyll

git add docs/
git commit -m "Deploy static site to docs/ on release - $(date)"
git push origin release --force

git checkout main

echo "Deployed to docs/ directory on release branch"
