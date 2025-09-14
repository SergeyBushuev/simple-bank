FROM amazoncorretto:21-alpine
WORKDIR /app
COPY bank-front/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]