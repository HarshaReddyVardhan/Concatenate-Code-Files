#!/bin/bash

# Function to print status
print_status() {
    if [ "$1" -eq 0 ]; then
        echo -e "\033[0;32m[OK]\033[0m $2"
    else
        echo -e "\033[0;31m[ERROR]\033[0m $2"
        if [ ! -z "$3" ]; then
            echo -e "      \033[0;33mHint:\033[0m $3"
        fi
        # Don't exit immediately, let the user see all checks
        return 1
    fi
}

echo "=========================================="
echo " Checking Docker Setup (Linux/macOS)"
echo "=========================================="

# 1. Check if Docker is installed
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    print_status 0 "Docker is installed: $DOCKER_VERSION"
    
    # 2. Check if Docker daemon is running
    if docker info &> /dev/null; then
        print_status 0 "Docker daemon is running."
    else
        print_status 1 "Docker daemon is NOT running." "Start Docker Desktop or the docker service (e.g., 'sudo systemctl start docker')."
    fi
else
    print_status 1 "Docker is NOT installed." "Install Docker from https://docs.docker.com/get-docker/"
fi

echo ""

# 3. Check for Docker Compose
if command -v docker-compose &> /dev/null; then
    COMPOSE_VERSION=$(docker-compose --version)
    print_status 0 "Docker Compose (standalone) is installed: $COMPOSE_VERSION"
elif docker compose version &> /dev/null; then
     COMPOSE_VERSION=$(docker compose version)
     print_status 0 "Docker Compose (plugin) is installed: $COMPOSE_VERSION"
else
    echo -e "\033[0;33m[WARN]\033[0m Docker Compose is NOT installed."
fi

echo ""
echo "=========================================="
