# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Exponer puerto
EXPOSE 8080

# Variables de entorno por defecto (se sobreescribirán en Render)
ENV SPRING_PROFILES_ACTIVE=prod

# Ejecutar aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]