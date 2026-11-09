# Stage 1: Build stage
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /build

COPY .mvn .mvn
COPY mvnw pom.xml
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B || true

COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Hardened, unprivileged runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app

# Create unprivileged system user for container security
RUN groupadd -r shortlink && useradd -r -g shortlink shortlink
USER shortlink:shortlink

COPY --from=builder --chown=shortlink:shortlink /build/target/shortlink-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]