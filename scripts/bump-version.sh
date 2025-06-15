#!/bin/bash

# Check if version argument is provided
if [ -z "$1" ]; then
    echo "Usage: ./bump-version.sh <new-version>"
    echo "Example: ./bump-version.sh 0.0.2-SNAPSHOT"
    exit 1
fi

NEW_VERSION=$1
CURRENT_VERSION=$(cat VERSION)

# Update VERSION file
echo $NEW_VERSION > VERSION

# Update pom.xml
sed -i '' "s/<version>$CURRENT_VERSION<\/version>/<version>$NEW_VERSION<\/version>/" pom.xml

# Update Docker image version in docker-compose.yaml if it exists
if [ -f docker-compose.yaml ]; then
    sed -i '' "s/image:.*:.*/image: post-generator:$NEW_VERSION/" docker-compose.yaml
fi

# Update Dockerfile version if it exists
if [ -f Dockerfile ]; then
    sed -i '' "s/ARG VERSION=.*/ARG VERSION=$NEW_VERSION/" Dockerfile
fi

echo "Version bumped from $CURRENT_VERSION to $NEW_VERSION"
echo "Files updated:"
echo "- VERSION"
echo "- pom.xml"
echo "- docker-compose.yaml (if exists)"
echo "- Dockerfile (if exists)" 