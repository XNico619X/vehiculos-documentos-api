# ---------- Etapa 1: compilar el JAR con Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Descargar dependencias primero (se cachean mientras pom.xml no cambie)
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# ---------- Etapa 2: imagen liviana solo con el JRE ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
