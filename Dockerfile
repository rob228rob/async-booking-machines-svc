# Этап сборки приложения
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /opt/app

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY ./src ./src

#RUN mvn test

RUN mvn clean package -DskipTests=true

FROM eclipse-temurin:17-jre-jammy

WORKDIR /opt/app

COPY --from=builder /opt/app/target/*.jar /opt/app/app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/opt/app/app.jar"]
