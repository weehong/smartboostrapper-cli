#!/bin/bash

# Script to build and push Docker image to DockerHub
set -e

# Determine script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Load environment variables from .env.docker
ENV_FILE="$SCRIPT_DIR/.env.docker"
if [ -f "$ENV_FILE" ]; then
    set -o allexport
    source "$ENV_FILE"
    set +o allexport
else
    echo "Error: .env.docker file not found at $ENV_FILE"
    echo "Please create one with required variables."
    exit 1
fi

# Validate required variables
if [ -z "$DOCKERHUB_USERNAME" ]; then
    echo "Error: DOCKERHUB_USERNAME not set in .env.docker"
    exit 1
fi

if [ -z "$IMAGE_NAME" ]; then
    echo "Error: IMAGE_NAME not set in .env.docker"
    exit 1
fi

# Set defaults for optional variables
VERSION="${VERSION:-latest}"
DOCKERFILE_PATH="${DOCKERFILE_PATH:-./Dockerfile}"
BUILD_CONTEXT="${BUILD_CONTEXT:-.}"
PLATFORMS="${PLATFORMS:-}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Docker Hub Build & Push Script ===${NC}"
echo -e "Project Root: ${YELLOW}$PROJECT_ROOT${NC}"
echo -e "Image: ${YELLOW}$DOCKERHUB_USERNAME/$IMAGE_NAME:$VERSION${NC}"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running${NC}"
    exit 1
fi

# Docker login
echo -e "${YELLOW}Logging in to DockerHub...${NC}"
if [ -n "$DOCKERHUB_PASSWORD" ]; then
    # Read password securely via stdin without exposing in process list
    echo -n "$DOCKERHUB_PASSWORD" | docker login -u "$DOCKERHUB_USERNAME" --password-stdin 2>/dev/null
else
    docker login -u "$DOCKERHUB_USERNAME"
fi

# Change to project root for build context
cd "$PROJECT_ROOT"

# Check if JAR file exists in target/ (required for Dockerfile)
if ! ls target/*.jar 1> /dev/null 2>&1; then
    echo -e "${RED}Error: No JAR file found in target/${NC}"
    echo -e "Please run '${YELLOW}mvn package${NC}' (or '${YELLOW}mvn package -DskipTests${NC}') first."
    exit 1
fi

# Build the image
echo -e "${YELLOW}Building Docker image...${NC}"

if [ -n "$PLATFORMS" ]; then
    # Multi-platform build using buildx
    echo -e "${YELLOW}Multi-platform build enabled: $PLATFORMS${NC}"

    # Ensure buildx builder exists
    if ! docker buildx inspect multiplatform-builder > /dev/null 2>&1; then
        echo -e "${YELLOW}Creating buildx builder...${NC}"
        docker buildx create --name multiplatform-builder --use
    else
        docker buildx use multiplatform-builder
    fi

    # Build and push multi-platform image
    docker buildx build \
        --file "$SCRIPT_DIR/$DOCKERFILE_PATH" \
        --platform "$PLATFORMS" \
        --tag "$DOCKERHUB_USERNAME/$IMAGE_NAME:$VERSION" \
        --tag "$DOCKERHUB_USERNAME/$IMAGE_NAME:latest" \
        --push \
        "$BUILD_CONTEXT"
else
    # Single-platform build for compatibility
    docker build \
        --file "$SCRIPT_DIR/$DOCKERFILE_PATH" \
        --tag "$DOCKERHUB_USERNAME/$IMAGE_NAME:$VERSION" \
        --tag "$DOCKERHUB_USERNAME/$IMAGE_NAME:latest" \
        "$BUILD_CONTEXT"

    # Push to DockerHub
    echo -e "${YELLOW}Pushing image to DockerHub...${NC}"
    docker push "$DOCKERHUB_USERNAME/$IMAGE_NAME:$VERSION"
    docker push "$DOCKERHUB_USERNAME/$IMAGE_NAME:latest"
fi

echo ""
echo -e "${GREEN}=== Build & Push Complete ===${NC}"
echo -e "Image available at: ${GREEN}$DOCKERHUB_USERNAME/$IMAGE_NAME:$VERSION${NC}"
echo ""
echo "To pull this image on another machine:"
echo -e "  ${YELLOW}docker pull $DOCKERHUB_USERNAME/$IMAGE_NAME:$VERSION${NC}"
echo ""
echo "To run the container:"
echo -e "  ${YELLOW}docker run -p 8100:8100 $DOCKERHUB_USERNAME/$IMAGE_NAME:$VERSION${NC}"
echo ""
echo "To use in docker-compose.yml:"
echo -e "  ${YELLOW}image: $DOCKERHUB_USERNAME/$IMAGE_NAME:$VERSION${NC}"
