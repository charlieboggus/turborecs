# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy Gradle wrapper + build scripts first for better layer caching.
COPY gradlew ./
COPY gradle ./gradle

# Copy build files (adjust if you use settings.gradle.kts at repo root)
COPY build.gradle.kts settings.gradle.kts ./

# Copy source
COPY src ./src

# Build a plain executable jar
RUN chmod +x ./gradlew && ./gradlew clean bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create a non-root user
RUN useradd -r -u 10001 spring
USER spring

# Copy the built jar
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

# Spring Boot default port
EXPOSE 8080

# Good defaults for containers (optional but sane)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]