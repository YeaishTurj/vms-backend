# ==============================================================================
# Multi-stage Dockerfile for VMS Spring Boot Backend (Render / Cloud Deployment)
# ==============================================================================

# Stage 1: Build the application artifact
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper files and pom.xml for dependency layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Pre-fetch dependencies
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and build executable JAR
COPY src/ ./src/
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal, secure JRE runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy JAR from build stage
COPY --from=build /app/target/vms-backend-0.0.1-SNAPSHOT.jar app.jar

# Server port (Render will inject $PORT environment variable)
EXPOSE 8081

# Container-aware memory settings
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
