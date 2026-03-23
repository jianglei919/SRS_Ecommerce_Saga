# Multi-stage build for Order Service
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml ../pom.xml
COPY order-service/pom.xml ./pom.xml
RUN cd .. && mvn clean install -DskipTests -q

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /root/.m2/repository /root/.m2/repository
COPY --from=builder /build/../order-service/target/order-service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
