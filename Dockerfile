FROM openjdk:17-jdk-alpine

COPY target/your-application.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]