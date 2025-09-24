#!/bin/bash

# Build the static site
echo "Building static site..."
clojure -X:build

# Navigate to project root
cd ../..

# Create docs directory if it doesn't exist
mkdir -p docs

# Copy built files to docs directory
echo "Copying files to docs directory..."
cp -r apps/web/public/* docs/

# Add and commit
git add docs/
git commit -m "Deploy static site to docs/ - $(date)"

# Push to main branch
git push origin main

echo "Deployed to docs/ directory for GitHub Pages!"