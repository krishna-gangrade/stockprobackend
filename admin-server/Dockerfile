# ── Stage 1: Build ─────────────────────────────────────────
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
RUN mvn dependency:go-offline -pl admin-server -am -DexcludeGroupIds=com.stockpro -q

# Copy source and build this service only
COPY admin-server/src ./admin-server/src/
RUN mvn package -pl admin-server -DskipTests -am -q

# ── Stage 2: Runtime ───────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S stockpro && adduser -S stockpro -G stockpro
USER stockpro

WORKDIR /app

COPY --from=builder /app/admin-server/target/*jar app.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]
