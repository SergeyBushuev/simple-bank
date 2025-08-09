FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY ../.. .
COPY ../commons .
RUN gradle :bank-transfer:build -x test

FROM amazoncorretto:21-alpine
WORKDIR /app
COPY bank-transfer/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]