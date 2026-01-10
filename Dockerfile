# Build Stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Environment variable to let the app know it's in Docker
ENV APP_IN_DOCKER=true

# Expose the port
EXPOSE 8080

# Volume for accessing host files
VOLUME /data

ENTRYPOINT ["java", "-jar", "app.jar"]
