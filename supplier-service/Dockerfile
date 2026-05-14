FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY payment-service/pom.xml ./payment-service/
COPY admin-server/pom.xml ./admin-server/
COPY api-gateway/pom.xml ./api-gateway/
COPY auth-service/pom.xml ./auth-service/
COPY eureka-server/pom.xml ./eureka-server/
COPY product-service/pom.xml ./product-service/
COPY purchase-service/pom.xml ./purchase-service/
COPY warehouse-service/pom.xml ./warehouse-service/
COPY alert-service/pom.xml ./alert-service/
COPY movement-service/pom.xml ./movement-service/
COPY report-service/pom.xml ./report-service/
COPY supplier-service/pom.xml ./supplier-service/

RUN mvn dependency:go-offline -pl supplier-service -am -DexcludeGroupIds=com.stockpro -q

COPY supplier-service/src ./supplier-service/src/

RUN mvn package -pl supplier-service -DskipTests -am -q

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S stockpro && adduser -S stockpro -G stockpro
USER stockpro

WORKDIR /app

COPY --from=builder /app/supplier-service/target/supplier-service-*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
