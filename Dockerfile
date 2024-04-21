# Use a base image with the JDK and Maven installed
FROM maven:3.8.4-openjdk-17-slim AS build

# Set the working directory in the container
WORKDIR /app

# Copy the project's pom.xml file
COPY pom.xml .

# Download dependencies and build the application
RUN mvn clean package

# Copy the application JAR file into the container
COPY target/websocketServer.jar /app/websocketServer.jar

# Use a smaller base image for runtime
FROM adoptopenjdk/openjdk17:alpine-jre

# Set the working directory in the container
WORKDIR /app

# Copy the application JAR file from the previous stage
COPY --from=build /app/websocketServer.jar /app/websocketServer.jar

# Expose the port that your Spring Boot application runs on
EXPOSE 8080

# Command to run your Spring Boot application when the container starts
CMD ["java", "-jar", "websocketServer.jar"]