#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}Starting backend test environment setup...${NC}"

# Change to project root directory
cd "$(dirname "$0")/.." || exit 1

# Load environment variables from .env file
if [ -f .env ]; then
    echo -e "${GREEN}Loading environment variables from .env...${NC}"
    export $(cat .env | grep -v '^#' | xargs)
else
    echo -e "${RED}Error: .env file not found${NC}"
    exit 1
fi

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running. Please start Docker Desktop.${NC}"
    exit 1
fi

# Start Docker services if they're not already running
echo -e "${GREEN}Starting Docker services...${NC}"
docker-compose up -d postgres rabbitmq

# Wait for PostgreSQL to be ready
echo -e "${YELLOW}Waiting for PostgreSQL to be ready...${NC}"
max_attempts=30
attempt=0
until docker exec $(docker-compose ps -q postgres) pg_isready -U ${POSTGRES_USER} > /dev/null 2>&1; do
    attempt=$((attempt+1))
    if [ $attempt -eq $max_attempts ]; then
        echo -e "${RED}PostgreSQL failed to start after $max_attempts attempts${NC}"
        exit 1
    fi
    echo -e "${YELLOW}Waiting for PostgreSQL... (attempt $attempt/$max_attempts)${NC}"
    sleep 1
done

echo -e "${GREEN}PostgreSQL is ready!${NC}"

# Run backend tests
echo -e "${GREEN}Running backend tests...${NC}"
cd backend
./gradlew test

# Capture exit code
TEST_EXIT_CODE=$?

echo ""
if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✓ Backend tests passed!${NC}"
else
    echo -e "${RED}✗ Backend tests failed!${NC}"
fi

exit $TEST_EXIT_CODE