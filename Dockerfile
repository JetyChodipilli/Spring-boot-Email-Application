FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B --no-transfer-progress verify

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --create-home --uid 10001 oys
COPY --from=build /app/target/oys-email-studio-1.0.0.jar /app/app.jar
USER oys
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
