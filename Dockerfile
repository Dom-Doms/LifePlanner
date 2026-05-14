FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENV APP_FRONTEND_URL=https://lifeplanner.gesu.gay
ENV SPRING_MAIL_HOST=
ENV SPRING_MAIL_PORT=587
ENV SPRING_MAIL_USERNAME=
ENV SPRING_MAIL_PASSWORD=

ENTRYPOINT ["java", "-jar", "app.jar"]
