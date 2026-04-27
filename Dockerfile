# =========================
# BUILD STAGE
# =========================
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# =========================
# RUNTIME STAGE
# =========================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Puerto dinámico (IMPORTANTE para Railway)
EXPOSE 8080

# Perfil prod por defecto
ENV SPRING_PROFILES_ACTIVE=prod

# IMPORTANTE: Railway usa variable PORT
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]