# Build Stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy pom.xml and download dependencies for efficient caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source files and build the production jar
COPY src ./src
RUN mvn clean package -DskipTests

# Production Runtime Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root system user for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create directory for resume uploads with appuser ownership
RUN mkdir -p /app/uploads/resumes && chown -R appuser:appgroup /app

# Copy built executable jar from builder stage
COPY --from=builder /app/target/outreach-0.0.1-SNAPSHOT.jar app.jar
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Expose backend application port
EXPOSE 8080

# Configure default environment variables
ENV SPRING_PROFILES_ACTIVE=dev
ENV PORT=8080

# Launch Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
