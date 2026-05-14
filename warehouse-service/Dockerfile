# ── Stage 1: Build ─────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy all POMs
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

# Cache dependencies (exclude internal modules to prevent resolution errors)
RUN mvn dependency:go-offline -pl warehouse-service -am -DexcludeGroupIds=com.stockpro -q

# Copy source of dependencies and this service
COPY product-service/src ./product-service/src/
COPY warehouse-service/src ./warehouse-service/src/

# Install dependencies into local repo and build this service
RUN mvn install -pl product-service,warehouse-service -DskipTests -am -q

# ── Stage 2: Runtime ───────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S stockpro && adduser -S stockpro -G stockpro
USER stockpro

WORKDIR /app

COPY --from=builder /app/warehouse-service/target/*exec.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
