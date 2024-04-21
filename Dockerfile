# Use a larger base image for building
FROM maven:3.8.4-openjdk-17 AS build

## Base image for runtime
#FROM eclipse-temurin:17-jdk-jammy

# Set the working directory in the container
WORKDIR /app

# Copy the project's pom.xml file
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline

# Copy the application source code
COPY src src

# Build the application
RUN mvn package -DskipTests

# Use a smaller base image for runtime
FROM eclipse-temurin:17-jdk-jammy

# Set the working directory in the container
WORKDIR /app

# Copy the application JAR file into the container
COPY target/WebSocket-Server-1.0.0-SNAPSHOT.jar /app/WebSocket-Server-1.0.0-SNAPSHOT.jar

ENTRYPOINT ["java","-jar","WebSocket-Server-1.0.0-SNAPSHOT.jar"]

