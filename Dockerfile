# ---- Build stage: compila o Quarkus a partir do código-fonte ----
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B package -DskipTests

# ---- Runtime stage: projeto empacota como uber-jar (quarkus.package.type=uber-jar no pom.xml) ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /deployments

COPY --from=build /workspace/target/*-runner.jar app.jar

EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
