FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY ../.. .
COPY ../commons .
RUN gradle :bank-configuration:build -x test

FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build bank-configuration/build/libs/*.jar app.jar
COPY bank-configuration/src/main/resources/configurations /app/configurations
ENTRYPOINT ["java", "-jar", "/app/app.jar"]