#!/bin/bash

echo "Building static site..."
clojure -X:build

cd ../..
git checkout -B gh-pages

mkdir -p docs

echo "Copying files to docs directory..."
cp -rL apps/web/public/* docs/

echo "Creating .nojekyll file..."
touch docs/.nojekyll

git add docs/
git commit -m "Deploy static site - $(date)"
git push origin gh-pages --force

git checkout main

echo "Deployed"
