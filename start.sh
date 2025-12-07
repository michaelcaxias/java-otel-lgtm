#!/bin/bash

echo "🚀 Starting Java OpenTelemetry LGTM Application"
echo "================================================"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Start Docker Compose services
echo "📦 Starting RabbitMQ, MongoDB, and Grafana LGTM..."
docker compose up -d

echo ""
echo "⏳ Waiting for services to be ready..."
sleep 5

echo ""
echo "✅ Services started successfully!"
echo ""
echo "📊 Access URLs:"
echo "   - Grafana LGTM:        http://localhost:3000 (admin/admin)"
echo "   - RabbitMQ Management: http://localhost:15672 (myuser/secret)"
echo "   - Application API:     http://localhost:8080"
echo ""
echo "🔧 Building application..."
./gradlew build -x test

echo ""
echo "🚀 Starting Spring Boot application..."
echo ""
./gradlew bootRun
