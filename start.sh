#!/bin/bash

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Source the .env file and export variables
if [ -f "$SCRIPT_DIR/.env" ]; then
    set -a  # automatically export all variables
    source "$SCRIPT_DIR/.env"
    set +a  # stop automatically exporting
else
    echo "Error: .env file not found at $SCRIPT_DIR/.env"
    exit 1
fi

# Start PostgreSQL container if not already running
cd "$SCRIPT_DIR"
if ! docker-compose ps postgres | grep -q "Up"; then
    echo "Starting PostgreSQL container..."
    docker-compose up -d postgres
   
    echo "Waiting for PostgreSQL to be ready..."
    timeout=30
    while [ $timeout -gt 0 ]; do
        if docker-compose ps postgres | grep -q "healthy"; then
            echo "PostgreSQL is ready!"
            break
        fi
        sleep 1
        timeout=$((timeout - 1))
    done
    
    if [ $timeout -eq 0 ]; then
        echo "Error: PostgreSQL container did not become healthy in time"
        exit 1
    fi
else
    echo "PostgreSQL container is already running"
fi

# Start RabbitMQ container if not already running
if ! docker-compose ps rabbitmq | grep -q "Up"; then
    echo "Starting RabbitMQ container..."
    docker-compose up -d rabbitmq
    
    echo "Waiting for RabbitMQ to be ready..."
    timeout=30
    while [ $timeout -gt 0 ]; do
        if docker-compose ps rabbitmq | grep -q "healthy"; then
            echo "RabbitMQ is ready!"
            break
        fi
        sleep 1
        timeout=$((timeout - 1))
    done
    
    if [ $timeout -eq 0 ]; then
        echo "Error: RabbitMQ container did not become healthy in time"
        exit 1
    fi
else
    echo "RabbitMQ container is already running"
fi

echo "Ensuring pgvector extension is enabled..."
docker-compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "CREATE EXTENSION IF NOT EXISTS vector;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "pgvector extension is ready"
else
    echo "Warning: Could not enable pgvector extension (it may already be enabled)"
fi

cd "$SCRIPT_DIR/backend"
./gradlew bootRun

