# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy POM files for dependency caching
COPY pom.xml .
COPY common/pom.xml common/
COPY service-parent/pom.xml service-parent/
COPY ingestion-service/pom.xml ingestion-service/
COPY intersection-service/pom.xml intersection-service/
COPY congestion-service/pom.xml congestion-service/
COPY routing-service/pom.xml routing-service/
COPY intersection-watchdog/pom.xml intersection-watchdog/

# Copy source trees
COPY common/src common/src
COPY service-parent service-parent
COPY ingestion-service/src ingestion-service/src
COPY intersection-service/src intersection-service/src
COPY congestion-service/src congestion-service/src
COPY routing-service/src routing-service/src
COPY intersection-watchdog/src intersection-watchdog/src

# Build all modules using BuildKit cache mount
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

# Runtime Stage: Ingestion Service
FROM eclipse-temurin:17-jre-alpine AS ingestion-service
WORKDIR /app
COPY --from=build /app/ingestion-service/target/ingestion-service.jar app.jar
EXPOSE 7020
ENTRYPOINT ["java", "-jar", "app.jar"]

# Runtime Stage: Intersection Service
FROM eclipse-temurin:17-jre-alpine AS intersection-service
WORKDIR /app
COPY --from=build /app/intersection-service/target/intersection-service.jar app.jar
EXPOSE 7021
ENTRYPOINT ["java", "-jar", "app.jar"]

# Runtime Stage: Congestion Service
FROM eclipse-temurin:17-jre-alpine AS congestion-service
WORKDIR /app
COPY --from=build /app/congestion-service/target/congestion-service.jar app.jar
EXPOSE 7022
ENTRYPOINT ["java", "-jar", "app.jar"]

# Runtime Stage: Routing Service
FROM eclipse-temurin:17-jre-alpine AS routing-service
WORKDIR /app
COPY --from=build /app/routing-service/target/routing-service.jar app.jar
EXPOSE 7023
ENTRYPOINT ["java", "-jar", "app.jar"]

# Runtime Stage: Intersection Watchdog
FROM eclipse-temurin:17-jre-alpine AS intersection-watchdog
WORKDIR /app
COPY --from=build /app/intersection-watchdog/target/intersection-watchdog.jar app.jar
EXPOSE 7024
ENTRYPOINT ["java", "-jar", "app.jar"]
