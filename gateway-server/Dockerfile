FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY ../.. .
COPY ../commons .
RUN gradle :gateway-server:build -x test

FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build gateway-server/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]