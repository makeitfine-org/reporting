# Multi-stage Dockerfile for reporting-service
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml ./
COPY ../pom.xml ../pom.xml

# Download dependencies (cacheable layer)
RUN apk add --no-cache maven 2>/dev/null || true

COPY src ./src

# Stage 2: Extract layers
FROM eclipse-temurin:21-jdk-alpine AS extractor
WORKDIR /build
COPY --from=builder /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy layers in correct order (most stable first)
COPY --from=extractor /build/dependencies/ ./
COPY --from=extractor /build/spring-boot-loader/ ./
COPY --from=extractor /build/snapshot-dependencies/ ./
COPY --from=extractor /build/application/ ./

# Labels
LABEL org.opencontainers.image.title="reporting-service"
LABEL org.opencontainers.image.description="Banking Reporting Microservice"
LABEL org.opencontainers.image.vendor="Banking Platform"

USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
