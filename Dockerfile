FROM openjdk:17-jdk

WORKDIR /app

COPY target/post-generator-1.0.0.jar /app/post-gen.jar

EXPOSE 8080

CMD ["java", "-jar", "post-gen.jar"]