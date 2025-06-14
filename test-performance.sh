#!/bin/bash

# Performance Testing Script for Thread Comparison
# This script tests different threading models using Apache Bench (ab)

# Configuration
REQUESTS=100000
CONCURRENCY=500
HOST="http://localhost:8080"
RESULTS_DIR="performance_results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

rm -rf $RESULTS_DIR
# Create results directory
mkdir -p $RESULTS_DIR

echo "=== Thread Performance Testing Started at $(date) ==="
echo "Configuration: $REQUESTS requests with $CONCURRENCY concurrent connections"
echo "Results will be saved in: $RESULTS_DIR"
echo ""

# Function to run ab test and save results
run_test() {
    local test_name=$1
    local url=$2
    local profile=$3

    echo "Running test: $test_name"
    echo "URL: $url"
    echo "Profile: $profile"
    echo "----------------------------------------"

    # Start the application with specific profile if provided
    if [ ! -z "$profile" ]; then
        echo "Starting application with profile: $profile"
        # Kill existing java processes (be careful in production!)
        pkill -f "performance-demo" || true
        sleep 2

        # Start application in background
        #java -jar target/performance-demo-1.0.0.jar --spring.profiles.active=$profile > $RESULTS_DIR/${test_name}_app.log 2>&1 &

       java -Xmx4G -Xms2G \
                -XX:+UseG1GC \
                -XX:NewRatio=3 \
                -XX:MaxGCPauseMillis=200 \
                -Dserver.tomcat.threads.max=600 \
                -Dserver.tomcat.threads.min-spare=30 \
                -Dserver.tomcat.accept-count=50 \
                -Dserver.tomcat.max-connections=600 \
                -Djava.security.egd=file:/dev/./urandom \
                -jar target/performance-demo-1.0.0.jar \
                --spring.profiles.active=$profile > $RESULTS_DIR/${test_name}_app.log 2>&1 &

       APP_PID=$!
        # Wait for application to start
        echo "Waiting for application to start... 1st attempt."
        sleep 6

        # Health check
        curl -f $HOST/actuator/health > /dev/null 2>&1
        if [ $? -ne 0 ]; then
            echo "Application health check failed. 2nd attempt. Waiting more..."
            sleep 6
        fi

        # Health check
        curl -f $HOST/actuator/health > /dev/null 2>&1
        if [ $? -ne 0 ]; then
           echo "Application health check failed. 3rd attempt. Waiting more..."
            sleep 8
        fi
    fi
    echo "Test is running..."
    # Run the actual test
    ab -l -n $REQUESTS -c $CONCURRENCY -g $RESULTS_DIR/${test_name}_gnuplot.dat $url > $RESULTS_DIR/${test_name}_results.txt 2>&1

    # Extract key metrics
    echo "Test completed. Key metrics:"
    grep "Requests per second" $RESULTS_DIR/${test_name}_results.txt
    grep "Time per request" $RESULTS_DIR/${test_name}_results.txt
    grep "Failed requests" $RESULTS_DIR/${test_name}_results.txt
    echo ""

    # Stop application if we started it
    if [ ! -z "$APP_PID" ]; then
        kill $APP_PID 2>/dev/null || true
        sleep 2
    fi
}

# Test 1: Java Synchronous with Platform Threads
run_test "java_sync_platform" "$HOST/java/sync?input=test" "platform-threads"

# Test 2: Java Synchronous with Virtual Threads
run_test "java_sync_virtual" "$HOST/java/virtual?input=test" "virtual-threads"

# Test 3: Java Asynchronous
run_test "java_async_platform" "$HOST/java/async?input=test" "platform-threads"

# Test 4: Kotlin Synchronous with Platform Threads
run_test "kotlin_sync_platform" "$HOST/kotlin/sync?input=test" "platform-threads"

# Test 5: Kotlin Synchronous with Virtual Threads
run_test "kotlin_sync_virtual" "$HOST/kotlin/virtual?input=test" "virtual-threads"

# Test 6: Kotlin Coroutines
run_test "kotlin_coroutines" "$HOST/kotlin/coroutine?input=test" "platform-threads"

echo "=== All tests completed at $(date) ==="
echo "Results saved in: $RESULTS_DIR"

# Generate summary report
echo "=== PERFORMANCE SUMMARY ===" > $RESULTS_DIR/summary_${TIMESTAMP}.txt
echo "Generated at: $(date)" >> $RESULTS_DIR/summary_${TIMESTAMP}.txt
echo "" >> $RESULTS_DIR/summary_${TIMESTAMP}.txt

for result_file in $RESULTS_DIR/*_results.txt; do
    test_name=$(basename $result_file _results.txt)
    echo "=== $test_name ===" >> $RESULTS_DIR/summary_${TIMESTAMP}.txt
    grep -A 5 "Requests per second" $result_file >> $RESULTS_DIR/summary_${TIMESTAMP}.txt
    echo "" >> $RESULTS_DIR/summary_${TIMESTAMP}.txt
done

echo "-------------------------------"
echo "Summary report generated: $RESULTS_DIR/summary_${TIMESTAMP}.txt"
