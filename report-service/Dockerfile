FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

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

RUN mvn dependency:go-offline -pl report-service -am -DexcludeGroupIds=com.stockpro -q

COPY report-service/src ./report-service/src/

RUN mvn package -pl report-service -DskipTests -am -q

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S stockpro && adduser -S stockpro -G stockpro
USER stockpro

WORKDIR /app

COPY --from=builder /app/report-service/target/report-service-*.jar app.jar

EXPOSE 8091

ENTRYPOINT ["java", "-jar", "app.jar"]
