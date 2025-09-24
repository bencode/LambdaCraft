#!/bin/bash

# Build the static site
echo "Building static site..."
clojure -X:build

# Create gh-pages branch if it doesn't exist
git checkout -B gh-pages

# Copy built files to root
cp -r public/* .

# Add and commit
git add .
git commit -m "Deploy static site - $(date)"

# Push to gh-pages branch
git push origin gh-pages --force

# Switch back to main
git checkout main

echo "Deployed to GitHub Pages!"