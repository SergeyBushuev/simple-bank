FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY ../.. .
COPY ../commons .
RUN gradle :bank-cash:build -x test

FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build bank-cash/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]