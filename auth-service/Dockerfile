# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy all POMs (parent + modules)
COPY pom.xml .
COPY admin-server/pom.xml ./admin-server/
COPY api-gateway/pom.xml ./api-gateway/
COPY auth-service/pom.xml ./auth-service/
COPY eureka-server/pom.xml ./eureka-server/
COPY product-service/pom.xml ./product-service/
COPY purchase-service/pom.xml ./purchase-service/
COPY payment-service/pom.xml ./payment-service/
COPY warehouse-service/pom.xml ./warehouse-service/
COPY alert-service/pom.xml ./alert-service/
COPY movement-service/pom.xml ./movement-service/
COPY report-service/pom.xml ./report-service/
COPY supplier-service/pom.xml ./supplier-service/

# Cache dependencies for this service
RUN mvn dependency:go-offline -pl auth-service -am -DexcludeGroupIds=com.stockpro -q

# Copy source and build this service only
COPY auth-service/src ./auth-service/src/
RUN mvn package -pl auth-service -DskipTests -am -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL maintainer="StockPro Platform Team <platform@stockpro.com>"
LABEL service="auth-service"
LABEL version="1.0.0"

# Non-root user for security
RUN addgroup -S stockpro && adduser -S stockpro -G stockpro
USER stockpro

WORKDIR /app

COPY --from=builder /app/auth-service/target/auth-service-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8083/api/v1/actuator/health || exit 1

EXPOSE 8083

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dnetworkaddress.cache.ttl=30", \
  "-Dnetworkaddress.cache.negative.ttl=0", \
  "-jar", "app.jar"]
