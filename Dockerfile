# Use a Java 17 base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the built Spring Boot JAR file into the container
# Assuming your build.gradle produces a JAR named 'main_backend_server-0.0.1-SNAPSHOT.jar'
# You might need to adjust this name based on your actual build output.
COPY build/libs/main_backend_server-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your Spring Boot application runs on
EXPOSE 8060

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
