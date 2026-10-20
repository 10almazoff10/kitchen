# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-25-alpine AS builder
WORKDIR /app

# Copy only the pom first to leverage Docker cache for Maven deps
COPY pom.xml .
# If a Maven wrapper exists, copy it too (optional)
# COPY mvnw .mvn/ ./

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src ./src

# Package the application (skip tests to speed up CI)
RUN mvn clean package -DskipTests -B

# ---------- Runtime stage ----------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Copy the built JAR from the builder image
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]