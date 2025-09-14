FROM amazoncorretto:21-alpine
WORKDIR /app
COPY bank-exchange-generator/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]