FROM amazoncorretto:21-alpine
WORKDIR /app
COPY bank-configuration/build/libs/*.jar app.jar
COPY bank-configuration/src/main/resources/configurations /app/configurations
ENTRYPOINT ["java", "-jar", "/app/app.jar"]