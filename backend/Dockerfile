FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY backend/pom.xml backend/
COPY backend ./
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
