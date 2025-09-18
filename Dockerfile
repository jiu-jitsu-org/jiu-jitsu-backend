# Build stage (JDK 17)
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew gradle/ /workspace/
RUN chmod +x /workspace/gradlew
COPY . /workspace
RUN ./gradlew clean bootJar --no-daemon

# Run stage (JRE 17)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]
