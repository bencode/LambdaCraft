#!/bin/bash

# Build the static site
echo "Building static site..."
clojure -X:build

# Navigate to project root
cd ../..

# Create gh-pages branch if it doesn't exist, or switch to it
git checkout -B gh-pages

# Create docs directory if it doesn't exist
mkdir -p docs

# Copy built files to docs directory
echo "Copying files to docs directory..."
cp -r apps/web/public/* docs/

# Add and commit
git add docs/
git commit -m "Deploy static site to docs/ on gh-pages - $(date)"

# Push to gh-pages branch
git push origin gh-pages --force

# Switch back to main
git checkout main

echo "Deployed to docs/ directory on gh-pages branch!"