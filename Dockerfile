# syntax=docker/dockerfile:1.7

# ---------- Build stage ----------
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q -DskipTests package \
    && cp target/*.jar app.jar

# ---------- Runtime stage ----------
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /workspace/app.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
