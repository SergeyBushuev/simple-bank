FROM amazoncorretto:21-alpine
WORKDIR /app
COPY bank-cash/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]