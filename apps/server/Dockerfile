ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
ARG JRE_IMAGE=eclipse-temurin:21-jre

FROM ${MAVEN_IMAGE} AS builder
WORKDIR /app
COPY apps/server/pom.xml .
RUN mvn dependency:go-offline
COPY apps/server/src ./src
RUN mvn package spring-boot:repackage -DskipTests

FROM ${JRE_IMAGE}
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
