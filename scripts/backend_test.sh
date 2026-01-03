#!/bin/bash

# Backend test runner script
# Usage:
#   ./scripts/backend_test.sh                    # Run all tests
#   ./scripts/backend_test.sh <file_path>        # Run tests for specific file
# Example:
#   ./scripts/backend_test.sh backend/src/test/kotlin/com/recall/backend/model/ChunkTest.kt
#   ./scripts/backend_test.sh src/test/kotlin/com/recall/backend/model/ChunkTest.kt

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

# Check if a file path was provided as argument
if [ -n "$1" ]; then
    # Convert file path to test class name
    # Supports multiple path formats:
    # - backend/src/test/kotlin/com/recall/backend/model/ChunkTest.kt
    # - src/test/kotlin/com/recall/backend/model/ChunkTest.kt
    # - com/recall/backend/model/ChunkTest.kt
    FILE_PATH="$1"
    
    # Remove backend/ prefix if present
    FILE_PATH="${FILE_PATH#backend/}"
    
    # Remove src/test/kotlin/ prefix if present
    FILE_PATH="${FILE_PATH#src/test/kotlin/}"
    
    # Remove .kt extension
    FILE_PATH="${FILE_PATH%.kt}"
    
    # Replace / with .
    TEST_CLASS="${FILE_PATH//\//.}"
    
    echo -e "${YELLOW}Running tests for: $TEST_CLASS${NC}"
    ./gradlew test --tests "$TEST_CLASS"
else
    echo -e "${YELLOW}Running all backend tests...${NC}"
    ./gradlew test
fi

# Capture exit code
TEST_EXIT_CODE=$?

echo ""
if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✓ Backend tests passed!${NC}"
else
    echo -e "${RED}✗ Backend tests failed!${NC}"
fi

exit $TEST_EXIT_CODE