#!/bin/bash

echo "==========================================="
echo "  Starting Concatenator in Docker..."
echo "==========================================="

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo -e "\033[0;31m[ERROR]\033[0m Docker is not running or not installed."
    echo "Please start Docker and try again."
    exit 1
fi

echo ""
echo "[1/3] Building Docker image..."

# Try docker-compose, fall back to docker compose
if command -v docker-compose &> /dev/null; then
    docker-compose build
    BUILD_EXIT_CODE=$?
else
    docker compose build
    BUILD_EXIT_CODE=$?
fi

if [ $BUILD_EXIT_CODE -ne 0 ]; then
    echo -e "\033[0;31m[ERROR]\033[0m Build failed."
    exit 1
fi

echo ""
echo "[2/3] Starting container..."
echo "The app will be available at: http://localhost:8080"
echo ""
echo "[NOTE] Your current folder is mounted to /data inside the container."
echo "       Please put your projects there or update docker-compose.yml."
echo ""

if command -v docker-compose &> /dev/null; then
    docker-compose up
else
    docker compose up
fi
