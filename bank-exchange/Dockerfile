FROM amazoncorretto:21-alpine
WORKDIR /app
COPY bank-exchange/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]