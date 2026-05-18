# ─── Etapa 1: Build con Maven ────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
# Descarga dependencias primero (cache friendly)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# ─── Etapa 2: Runtime ────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# FIX: instalar wget para que el healthcheck de docker-compose funcione
RUN apk add --no-cache wget

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
