# Multi-stage Dockerfile for Thread Performance Testing

# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

# Install Apache Bench and monitoring tools
RUN apt-get update && \
    apt-get install -y \
    apache2-utils \
    curl \
    htop \
    procps \
    net-tools \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/target/performance-demo-1.0.0.jar app.jar

# Copy test scripts
COPY test-performance.sh .
RUN chmod +x test-performance.sh

# Create results directory
RUN mkdir -p performance_results

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Default command
CMD ["java", "-jar", "app.jar"]

# Environment variables for JVM tuning
ENV JAVA_OPTS="-Xmx2G -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE="platform-threads"

# Optional: Create different startup scripts for different profiles
COPY <<EOF /app/start-platform.sh
#!/bin/bash
export SPRING_PROFILES_ACTIVE=platform-threads
java \$JAVA_OPTS -jar app.jar
EOF

COPY <<EOF /app/start-virtual.sh
#!/bin/bash
export SPRING_PROFILES_ACTIVE=virtual-threads
java \$JAVA_OPTS -jar app.jar
EOF

RUN chmod +x /app/start-*.sh