# Individual Apache Bench Test Commands

## Prerequisites
1. Build the application: `mvn clean package`
2. Install Apache Bench: `sudo apt-get install apache2-utils` (Ubuntu) or `brew install apache2` (macOS)

## Test Commands

### 1. Java Platform Threads (Synchronous)
```bash
# Start application
java -jar target/performance-demo-1.0.0.jar --spring.profiles.active=platform-threads

# Test command
ab -l -n 100000 -c 100 -g java_sync_platform.dat http://localhost:8080/java/sync?input=test
```

### 2. Java Virtual Threads (Synchronous)
```bash
# Start application
java -jar target/performance-demo-1.0.0.jar --spring.profiles.active=virtual-threads

# Test command
ab -l -n 100000 -c 100 -g java_sync_virtual.dat http://localhost:8080/java/virtual?input=test
```

### 3. Java Asynchronous (CompletableFuture)
```bash
# Start application
java -jar target/performance-demo-1.0.0.jar --spring.profiles.active=platform-threads

# Test command
ab -l -n 100000 -c 100 -g java_async.dat http://localhost:8080/java/async?input=test
```

### 4. Kotlin Platform Threads (Synchronous)
```bash
# Start application
java -jar target/performance-demo-1.0.0.jar --spring.profiles.active=platform-threads

# Test command
ab -l -n 100000 -c 100 -g kotlin_sync_platform.dat http://localhost:8080/kotlin/sync?input=test
```

### 5. Kotlin Virtual Threads (Synchronous)
```bash
# Start application
java -jar target/performance-demo-1.0.0.jar --spring.profiles.active=virtual-threads

# Test command
ab -l -n 100000 -c 100 -g kotlin_sync_virtual.dat http://localhost:8080/kotlin/virtual?input=test
```

### 6. Kotlin Coroutines
```bash
# Start application
java -jar target/performance-demo-1.0.0.jar --spring.profiles.active=platform-threads

# Test command
ab -l -n 100000 -c 100 -g kotlin_coroutines.dat http://localhost:8080/kotlin/coroutine?input=test
```

## Test Variations

### Lower Concurrency Test (for baseline comparison)
```bash
ab -l -n 100000 -c 10 http://localhost:8080/java/sync?input=test
```

### Higher Concurrency Test (stress test)
```bash
ab -l -n 100000 -c 500 http://localhost:8080/java/virtual?input=test
```

### Extended Test (more requests)
```bash
ab -l -n 500000 -c 100 http://localhost:8080/kotlin/coroutine?input=test
```

## Monitoring During Tests

### JVM Thread Monitoring
```bash
# Monitor thread count in real-time
watch -n 1 'curl -s http://localhost:8080/actuator/metrics/jvm.threads.live | jq .measurements[0].value'

# Get thread dump
curl -s http://localhost:8080/actuator/threaddump > threaddump_$(date +%s).json
```

### System Resource Monitoring
```bash
# Monitor CPU and memory usage
top -pid $(pgrep -f performance-demo)

# Monitor with htop (if available)
htop -p $(pgrep -f performance-demo)
```

## Result Analysis

### Key Metrics to Compare
- **Requests per second (RPS)**: Higher is better
- **Time per request (mean)**: Lower is better
- **Time per request (mean, across all concurrent requests)**: Lower is better
- **Failed requests**: Should be 0
- **95th percentile response time**: Lower is better

### Analyzing Results
```bash
# Extract key metrics from ab output
grep "Requests per second" results.txt
grep "Time per request" results.txt
grep "Failed requests" results.txt

# Generate performance graphs (if gnuplot is installed)
gnuplot -e "
set terminal png;
set output 'performance_graph.png';
plot 'java_sync_platform.dat' using 9 with lines title 'Java Platform',
     'java_sync_virtual.dat' using 9 with lines title 'Java Virtual',
     'kotlin_coroutines.dat' using 9 with lines title 'Kotlin Coroutines'
"
```