FROM maven:3.9.5-eclipse-temurin-21 AS build

WORKDIR /app
COPY Resume-Analyser-main/ ./Resume-Analyser-main/
WORKDIR /app/Resume-Analyser-main
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app
COPY --from=build /app/Resume-Analyser-main/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
