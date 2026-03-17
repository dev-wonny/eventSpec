FROM gradle:8.10-jdk21 AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

