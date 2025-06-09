FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the jar
COPY build/libs/salesmanagement-0.0.1-SNAPSHOT.jar /app/salesmanagement.jar

# Copy application.properties correctly
COPY src/main/resources/application.properties /app/application.properties

# Expose port
EXPOSE 8080

# Correct entrypoint with proper config location
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "/app/salesmanagement.jar", "--spring.config.location=file:/app/application.properties"]
