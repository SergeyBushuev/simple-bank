FROM amazoncorretto:21-alpine
WORKDIR /app
COPY gateway-server/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]