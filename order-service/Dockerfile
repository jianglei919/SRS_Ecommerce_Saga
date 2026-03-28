# Multi-stage build for Order Service
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY . .
RUN mvn clean install -pl order-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /root/.m2/repository /root/.m2/repository
COPY --from=builder /workspace/order-service/target/order-service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
