# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Download dependencies first to leverage Docker layer caching
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

# Copy source and build the executable Spring Boot JAR
COPY src src
RUN mvn -B -DskipTests package && cp target/*.jar app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as an unprivileged user
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser
COPY --from=build --chown=appuser:appgroup /app/app.jar app.jar
USER appuser

EXPOSE 8080

# Stay within container memory limits (e.g., Render 512MB free/starter plans)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "app.jar"]
