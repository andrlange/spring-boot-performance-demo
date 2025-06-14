#!/bin/bash

# Bean verification script
# This script checks if Java beans are properly loaded

HOST="http://localhost:8080"

echo "=== Spring Boot Bean Verification ==="
echo "Checking if Java and Kotlin beans are properly loaded..."
echo ""

# Check if application is running
curl -f $HOST/actuator/health > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ Application is not running!"
    echo "Please start the application first:"
    echo "java -jar target/performance-demo-1.0.0.jar"
    exit 1
fi

echo "✅ Application is running"
echo ""

# Check diagnostic endpoints
echo "=== Bean Status Check ==="
echo "Checking Java beans status..."
curl -s "$HOST/diagnostic/java-status" | jq . 2>/dev/null || curl -s "$HOST/diagnostic/java-status"
echo ""

echo "=== All Controllers ==="
curl -s "$HOST/diagnostic/controllers" | jq . 2>/dev/null || curl -s "$HOST/diagnostic/controllers"
echo ""

echo "=== All Services ==="
curl -s "$HOST/diagnostic/services" | jq . 2>/dev/null || curl -s "$HOST/diagnostic/services"
echo ""

# Test Java endpoints directly
echo "=== Direct Java Endpoint Tests ==="

echo "Testing Java Controller endpoints..."

# Test Java sync endpoint
echo "Java Sync endpoint:"
curl -s -w "HTTP Status: %{http_code}\n" "$HOST/java/sync?input=test" || echo "❌ Failed to reach Java sync endpoint"
echo ""

# Test Java health endpoint
echo "Java Health endpoint:"
curl -s -w "HTTP Status: %{http_code}\n" "$HOST/java/health" || echo "❌ Failed to reach Java health endpoint"
echo ""

# Test Kotlin endpoints for comparison
echo "=== Kotlin Endpoint Tests (for comparison) ==="

echo "Kotlin Sync endpoint:"
curl -s -w "HTTP Status: %{http_code}\n" "$HOST/kotlin/sync?input=test" || echo "❌ Failed to reach Kotlin sync endpoint"
echo ""

# Check component scanning
echo "=== Component Scan Analysis ==="
echo "All relevant beans:"
curl -s "$HOST/diagnostic/beans" | jq '.relevantBeans' 2>/dev/null || curl -s "$HOST/diagnostic/beans"
echo ""

echo "=== Troubleshooting Tips ==="
echo "If Java beans are not found:"
echo "1. Check that Java source files are in: src/main/java/cool/cfapps/performancedemo/"
echo "2. Verify @Service and @RestController annotations are present"
echo "3. Rebuild with: mvn clean package"
echo "4. Check application logs for component scanning errors"
echo "5. Try explicit component scanning in PerformanceDemoApplication.kt"