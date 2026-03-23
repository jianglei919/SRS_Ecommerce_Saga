# Multi-stage build for Inventory Service
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml ../pom.xml
COPY inventory-service/pom.xml ./pom.xml
RUN cd .. && mvn clean install -DskipTests -q

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /root/.m2/repository /root/.m2/repository
COPY --from=builder /build/../inventory-service/target/inventory-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
